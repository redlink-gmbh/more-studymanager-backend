/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.repository;

import com.google.common.base.Supplier;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.model.Participant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.redlink.more.studymanager.model.gateway.RoutingInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.redlink.more.studymanager.repository.RepositoryUtils.*;

@Component
public class ParticipantRepository {

    private static final String INSERT_PARTICIPANT_AND_TOKEN =
            "WITH p AS (INSERT INTO participants(study_id,participant_id,alias,study_group_id) VALUES (:study_id,(SELECT COALESCE(MAX(participant_id),0)+1 FROM participants WHERE study_id = :study_id),:alias,:study_group_id) RETURNING participant_id, study_id) INSERT INTO registration_tokens(participant_id,study_id,token) SELECT participant_id, study_id, :token FROM p";
    private static final String UPDATE_REGISTRATION_TOKEN = """
            INSERT INTO registration_tokens(study_id, participant_id, token)
            VALUES (:study_id, :participant_id, :token)
            ON CONFLICT (study_id, participant_id) DO UPDATE SET token = excluded.token
            """;
    private static final String GET_PARTICIPANT_BY_IDS =
            "SELECT " +
            "    p.participant_id, p.study_id, p.alias, p.study_group_id, r.token as token, p.status, p.created, " +
            "    p.modified, p.start, ARRAY_AGG(pog.observation_group_id) FILTER (WHERE pog.observation_group_id IS NOT NULL) AS observation_group_ids " +
            "FROM participants p " +
            "    LEFT JOIN registration_tokens r ON p.study_id = r.study_id AND p.participant_id = r.participant_id " +
            "    LEFT JOIN participant_observation_groups pog ON p.study_id = pog.study_id AND p.participant_id = pog.participant_id " +
            "WHERE p.study_id = ? AND p.participant_id = ? " +
            "GROUP BY p.study_id, p.participant_id, r.token";
    private static final String LIST_PARTICIPANTS_BY_STUDY =
            "SELECT " +
            "    p.participant_id, p.study_id, p.alias, p.study_group_id, r.token as token, p.status, p.created, " +
            "    p.modified, p.start, ARRAY_AGG(pog.observation_group_id) FILTER (WHERE pog.observation_group_id IS NOT NULL) AS observation_group_ids " +
            "FROM participants p " +
            "    LEFT JOIN registration_tokens r ON p.study_id = r.study_id AND p.participant_id = r.participant_id " +
            "    LEFT JOIN participant_observation_groups pog ON p.study_id = pog.study_id AND p.participant_id = pog.participant_id " +
            "WHERE p.study_id = ? " +
            "GROUP BY p.study_id, p.participant_id, r.token";
    /*
     * NOTE: parsing NULL as observation_group_ids will deactivate the filter. parsing [] will only list participants
     * with no observation group. Otherwise, Participants with any of the parsed observation groups will be returned
     */
    private static final String LIST_PARTICIPANTS_BY_STUDY_AND_GROUPS =
            """
                    SELECT
                        p.participant_id, p.study_id, p.alias, p.status, p.created, p.start, p.modified,\s
                        r.token as token, sg.study_group_id, sg.title as study_group_title,\s
                        ARRAY_AGG(pog.observation_group_id) FILTER (WHERE pog.observation_group_id IS NOT NULL) AS observation_group_ids\s
                    FROM participants p\s
                        LEFT JOIN registration_tokens r ON p.study_id = r.study_id AND p.participant_id = r.participant_id\s
                        LEFT OUTER JOIN study_groups sg ON ( p.study_id = sg.study_id AND p.study_group_id = sg.study_group_id )\s
                        LEFT JOIN participant_observation_groups pog ON p.study_id = pog.study_id AND p.participant_id = pog.participant_id\s
                    WHERE p.study_id = :study_id\s
                    AND (p.study_group_id = :study_group_id OR :study_group_id::INT IS NULL)\s
                    GROUP BY p.study_id, p.participant_id, sg.study_group_id, sg.title, r.token\s
                    HAVING (:observation_group_ids::INT[] IS NULL)\s
                        OR COUNT(pog.observation_group_id) = 0\s
                        OR COUNT(CASE WHEN pog.observation_group_id = ANY(:observation_group_ids) THEN 1 END) > 0;""";
    private static final String DELETE_PARTICIPANT =
            "DELETE FROM participants " +
            "WHERE study_id=? AND participant_id=?";
    private static final String UPDATE_PARTICIPANT =
            "UPDATE participants " +
            "SET alias = :alias, study_group_id = :study_group_id, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id";
    private static final String SET_STATUS =
            "UPDATE participants p SET status = :status::participant_status, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id " +
            "RETURNING *, " +
            "    (SELECT token FROM registration_tokens t WHERE t.study_id = p.study_id AND t.participant_id = p.participant_id ) as token, " +
            "    (SELECT ARRAY_AGG(observation_group_id) FROM participant_observation_groups pog WHERE pog.study_id = p.study_id AND pog.participant_id = p.participant_id ) as observation_group_ids";
    private static final String SET_STATUS_IF =
            "UPDATE participants p SET status= :new_status::participant_status, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id " +
            "   AND status = :current_status::participant_status " +
            "RETURNING *, (SELECT token FROM registration_tokens t WHERE t.study_id = p.study_id AND t.participant_id = p.participant_id ) as token";

    private static final String LIST_PARTICIPANTS_FOR_CLOSING =
            "SELECT DISTINCT p.*, 't' as token, ARRAY_AGG(pog.observation_group_id) AS observation_group_ids " +
            "FROM studies s " +
            "    JOIN participants p ON s.study_id = p.study_id " +
            "    LEFT JOIN study_groups sg ON p.study_group_id = sg.study_group_id AND p.study_id = sg.study_id " +
            "    LEFT JOIN participant_observation_groups pog ON p.study_id = pog.study_id AND p.participant_id = pog.participant_id " +
            "WHERE s.status = 'active' " +
            "  AND p.status = 'active' " +
            "  AND  p.start IS NOT NULL " +
            "  AND COALESCE(sg.duration, s.duration) IS NOT NULL " +
            "  AND (p.start + ((COALESCE(sg.duration, s.duration)->>'value')::int || ' ' || (COALESCE(sg.duration, s.duration)->>'unit'))::interval) < NOW()" +
            "GROUP BY p.study_id, p.participant_id";

    private static final String GET_ROUTING_INFO = """
            SELECT pt.study_id as study_id, pt.participant_id as participant_id, study_group_id,
                s.status IN ('active', 'preview') as study_active,
                pt.status = 'active' as participant_active,
                (SELECT ARRAY_AGG(pog.observation_group_id)
                          FROM participant_observation_groups pog
                          WHERE pog.study_id = pt.study_id AND pog.participant_id = pt.participant_id) AS observation_group_ids
            FROM participants pt
                INNER JOIN studies s on (s.study_id = pt.study_id)
            WHERE pt.study_id = ? AND pt.participant_id = ?
            """;
    /*
     * SQL Statements for managing participant_observation_groups mapping for participants
     */
    private static final String DELETE_PARTICIPANT_OBSERVATION_GROUP_IDS =
            "DELETE FROM participant_observation_groups " +
                    "WHERE study_id = :study_id AND participant_id = :participant_id;";

    private static final String SET_PARTICIPANT_OBSERVATION_GROUP_IDS =
            "INSERT INTO participant_observation_groups (study_id, participant_id, observation_group_id) " +
                    "SELECT :study_id, :participant_id, unnest(:observation_group_ids::int[]);";

    private static final String DELETE_ALL = "DELETE FROM participants";
    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedTemplate;

    public ParticipantRepository(JdbcTemplate template) {
        this.template = template;
        this.namedTemplate = new NamedParameterJdbcTemplate(template);
    }

    @Transactional
    public Participant insert(Participant participant) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            namedTemplate.update(INSERT_PARTICIPANT_AND_TOKEN, toParams(participant).addValue("token", participant.getRegistrationToken()), keyHolder, new String[]{"participant_id"});
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Study " + participant.getStudyId() + " does not exist");
        }
        Integer participantId = keyHolder.getKey().intValue();
        setParticipantObservationGroupIds(participant.getStudyId(), participantId, participant.getObservationGroupIds());
        return getByIds(participant.getStudyId(), participantId);
    }

    public Participant getByIds(long studyId, int participantId) {
        try {
            return template.queryForObject(GET_PARTICIPANT_BY_IDS, getParticipantRowMapper(), studyId, participantId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Participant> listParticipants(Long studyId) {
        return template.query(LIST_PARTICIPANTS_BY_STUDY, getParticipantRowMapper(), studyId);
    }

    public List<Participant> listParticipants(Long studyId, Integer studyGroupId, Set<Integer> observationGroupIds) {
        return namedTemplate.query(
                LIST_PARTICIPANTS_BY_STUDY_AND_GROUPS,
                new MapSqlParameterSource()
                        .addValue("study_id", studyId)
                        .addValue("study_group_id", studyGroupId)
                        .addValue("observation_group_ids", observationGroupIds == null ? null : observationGroupIds.toArray(new Integer[0])),
                getParticipantRowMapper());
    }

    public List<Participant> listParticipantsForClosing() {
        return template.query(LIST_PARTICIPANTS_FOR_CLOSING, getParticipantRowMapper());
    }

    @Transactional
    public void deleteParticipant(Long studyId, Integer participantId) {
        template.update(DELETE_PARTICIPANT, studyId, participantId);
    }

    /**
     * Updates the participant and the {@link Participant#getObservationGroupIds()}
     * @param participant
     * @return the updated participant as stored in the database
     */
    @Transactional
    public Participant update(Participant participant) {
        namedTemplate.update(UPDATE_PARTICIPANT, toParams(participant).addValue("participant_id", participant.getParticipantId()));
        setParticipantObservationGroupIds(participant.getStudyId(), participant.getParticipantId(), participant.getObservationGroupIds());
        return getByIds(participant.getStudyId(), participant.getParticipantId());
    }

    @Transactional
    public Optional<Participant> setStatusByIds(Long studyId, Integer participantId, Participant.Status status) {
        return namedTemplate.query(SET_STATUS,
                toParams(studyId, participantId)
                        .addValue("status", RepositoryUtils.toParam(status)),
                getParticipantRowMapper()
        ).stream().findFirst();
    }

    @Transactional
    public void cleanupParticipant(Long studyId, Integer participantId) {
        final var params = toParams(studyId, participantId);
        namedTemplate.update("DELETE FROM api_credentials WHERE study_id = :study_id AND participant_id = :participant_id", params);
        namedTemplate.update("DELETE FROM registration_tokens WHERE study_id = :study_id AND participant_id = :participant_id", params);
        namedTemplate.update("DELETE FROM push_notifications_token WHERE study_id = :study_id AND participant_id = :participant_id", params);
    }

    @Transactional
    public void cleanupParticipants(Long studyId) {
        final var params = toParams(studyId);
        namedTemplate.update("DELETE FROM api_credentials WHERE study_id = :study_id", params);
        namedTemplate.update("DELETE FROM registration_tokens WHERE study_id = :study_id", params);
        namedTemplate.update("DELETE FROM push_notifications_token WHERE study_id = :study_id", params);
    }

    @Transactional
    public void resetParticipants(final Long studyId, final Supplier<String> tokenSource) {
        // First clear credentials and tokens...
        cleanupParticipants(studyId);
        // ... then reset participant-status and start-date ...
        final var pIDs = namedTemplate.query(
                "UPDATE participants SET status = DEFAULT, start = NULL WHERE study_id = :study_id RETURNING *",
                toParams(studyId),
                intReader("participant_id")
        );
        // ... and finally create new token for the participants
        namedTemplate.batchUpdate(
                UPDATE_REGISTRATION_TOKEN,
                pIDs.stream()
                        .map(pid -> toParams(studyId, pid).addValue("token", tokenSource.get()))
                        .toArray(MapSqlParameterSource[]::new)
        );
    }

    public void clear() {
        template.update(DELETE_ALL);
    }

    private static MapSqlParameterSource toParams(Long studyId) {
        return new MapSqlParameterSource()
                .addValue("study_id", studyId)
                ;
    }

    private static MapSqlParameterSource toParams(Long studyId, Integer participantId) {
        return toParams(studyId)
                .addValue("participant_id", participantId)
                ;
    }

    private static MapSqlParameterSource toParams(Participant participant) {
        return toParams(participant.getStudyId())
                .addValue("alias", participant.getAlias())
                .addValue("study_group_id", participant.getStudyGroupId());
    }

    private static RowMapper<Participant> getParticipantRowMapper() {
        return (rs, rowNum) -> new Participant()
                .setStudyId(rs.getLong("study_id"))
                .setParticipantId(rs.getInt("participant_id"))
                .setAlias(rs.getString("alias"))
                .setStudyGroupId(readNullableInteger(rs, "study_group_id"))
                .setCreated(RepositoryUtils.readInstant(rs, "created"))
                .setModified(RepositoryUtils.readInstant(rs, "modified"))
                .setStatus(RepositoryUtils.readParticipantStatus(rs, "status"))
                .setStart(RepositoryUtils.readInstant(rs, "start"))
                .setRegistrationToken(rs.getString("token"))
                .setObservationGroupIds(RepositoryUtils.readSet(rs, "observation_group_ids", Integer.class));
    }

    private void setParticipantObservationGroupIds(Long studyId, Integer participantId, Set<Integer> observationGroupIds) {
        final var params = toParams(studyId, participantId);
        namedTemplate.update(DELETE_PARTICIPANT_OBSERVATION_GROUP_IDS, params);
        if(observationGroupIds != null && !observationGroupIds.isEmpty()) {
            params.addValue("observation_group_ids", observationGroupIds.toArray(new Integer[0]));
            namedTemplate.update(SET_PARTICIPANT_OBSERVATION_GROUP_IDS, params);
        }
    }
    public Optional<RoutingInfo> getRoutingInfo(Long studyId, Integer participantId) {
        try (var stream = template.queryForStream(GET_ROUTING_INFO, getRoutingInfoMapper(), studyId, participantId)) {
            return stream.findFirst();
        }
    }
    private static RowMapper<RoutingInfo> getRoutingInfoMapper() {
        return ((row, rowNum) ->
                new RoutingInfo(
                        row.getLong("study_id"),
                        row.getInt("participant_id"),
                        RepositoryUtils.readNullableInteger(row, "study_group_id"),
                        RepositoryUtils.readSet(row, "observation_group_ids", Integer.class),
                        row.getBoolean("study_active"),
                        row.getBoolean("participant_active")
                )
        );
    }

}
