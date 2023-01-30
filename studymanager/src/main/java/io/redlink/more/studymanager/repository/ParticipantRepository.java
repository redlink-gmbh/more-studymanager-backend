package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.model.Participant;
import java.util.List;
import java.util.Optional;
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

import static io.redlink.more.studymanager.repository.RepositoryUtils.getValidNullableIntegerValue;
import static io.redlink.more.studymanager.repository.RepositoryUtils.toParam;

@Component
public class ParticipantRepository {

    private static final String INSERT_PARTICIPANT_AND_TOKEN =
            "WITH p AS (INSERT INTO participants(study_id,participant_id,alias,study_group_id) VALUES (:study_id,(SELECT COALESCE(MAX(participant_id),0)+1 FROM participants WHERE study_id = :study_id),:alias,:study_group_id) RETURNING participant_id, study_id) INSERT INTO registration_tokens(participant_id,study_id,token) SELECT participant_id, study_id, :token FROM p";
    private static final String GET_PARTICIPANT_BY_IDS = "SELECT p.participant_id, p.study_id, p.alias, p.study_group_id, r.token as token, p.status, p.created, p.modified FROM participants p LEFT JOIN registration_tokens r ON p.study_id = r.study_id AND p.participant_id = r.participant_id WHERE p.study_id = ? AND p.participant_id = ?";
    private static final String LIST_PARTICIPANTS_BY_STUDY = "SELECT p.participant_id, p.study_id, p.alias, p.study_group_id, r.token as token, p.status, p.created, p.modified FROM participants p LEFT JOIN registration_tokens r ON p.study_id = r.study_id AND p.participant_id = r.participant_id WHERE p.study_id = ?";
    private static final String DELETE_PARTICIPANT =
            "DELETE FROM participants " +
            "WHERE study_id=? AND participant_id=? AND status = 'new'";
    private static final String UPDATE_PARTICIPANT =
            "UPDATE participants " +
            "SET alias = :alias, study_group_id = :study_group_id, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id";
    private static final String SET_STATUS =
            "UPDATE participants p SET status = :status::participant_status, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id " +
            "RETURNING *, (SELECT token FROM registration_tokens t WHERE t.study_id = p.study_id AND t.participant_id = p.participant_id ) as token";
    private static final String SET_STATUS_IF =
            "UPDATE participants p SET status= :new_status::participant_status, modified = now() " +
            "WHERE study_id = :study_id AND participant_id = :participant_id " +
            "   AND status = :current_status::participant_status " +
            "RETURNING *, (SELECT token FROM registration_tokens t WHERE t.study_id = p.study_id AND t.participant_id = p.participant_id ) as token";
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
        return getByIds(participant.getStudyId(), keyHolder.getKey().intValue());
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

    @Transactional
    public Optional<Participant> deleteParticipant(Long studyId, Integer participantId) {
        var deletedCount = template.update(DELETE_PARTICIPANT, studyId, participantId);
        if (deletedCount > 0) {
            // Successfully deleted participant with state "new"
            return Optional.empty();
        }

        // Not deleted, so we try to transition status from "active" to "kicked_out"
        final Optional<Participant> kickedOut = namedTemplate.query(SET_STATUS_IF,
                toParams(studyId, participantId)
                        .addValue("current_status", toParam(Participant.Status.ACTIVE))
                        .addValue("new_status", toParam(Participant.Status.KICKED_OUT)),
                getParticipantRowMapper()
        ).stream().findFirst();
        if (kickedOut.isPresent()) {
            // "transition was successful, let's do some cleanup
            cleanupParticipant(studyId, participantId);
            return kickedOut;
        }

        // Else
        return Optional.ofNullable(getByIds(studyId, participantId));
    }

    public Participant update(Participant participant) {
        namedTemplate.update(UPDATE_PARTICIPANT, toParams(participant).addValue("participant_id", participant.getParticipantId()));
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
                .setStudyGroupId(getValidNullableIntegerValue(rs, "study_group_id"))
                .setCreated(rs.getTimestamp("created"))
                .setModified(rs.getTimestamp("modified"))
                .setStatus(RepositoryUtils.readParticipantStatus(rs, "status"))
                .setRegistrationToken(rs.getString("token"));
    }
}
