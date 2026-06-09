/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.auth.token.repository;

import io.redlink.more.auth.token.model.LoginToken;
import io.redlink.more.auth.token.model.RoutingInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LoginTokenRepository {

    private static final String DELETE_SALT =
            "DELETE FROM salt_tokens WHERE study_id = ? AND participant_id = ?";
    private static final String DELETE_BY_PARTICIPANT =
            "DELETE FROM login_tokens WHERE study_id = ? AND participant_id = ?";

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


    private final JdbcTemplate template;

    public LoginTokenRepository(JdbcTemplate template) {
        this.template = template;
    }

    public void deleteLoginTokens(Long studyId, Integer participantId) {
        if (participantId == null || studyId == null) {
            return;
        }
        template.update(DELETE_BY_PARTICIPANT, studyId, participantId);
        template.update(DELETE_SALT, studyId, participantId);
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
                        DBUtils.readOptionalInt(row, "study_group_id"),
                        DBUtils.readSet(row, "observation_group_ids", Integer.class),
                        row.getBoolean("study_active"),
                        row.getBoolean("participant_active")
                )
        );
    }


    private RowMapper<LoginToken> getRowMapper() {
        return (rs, rowNum) -> new LoginToken()
                .setStudyId(rs.getLong("study_id"))
                .setParticipantId(rs.getInt("participant_id"))
                .setApplication(rs.getString("application"))
                .setCode(rs.getString("code"))
                .setCodeHash(rs.getString("code_hash"));
    }


}
