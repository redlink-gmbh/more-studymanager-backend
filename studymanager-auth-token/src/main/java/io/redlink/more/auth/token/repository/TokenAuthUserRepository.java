/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Oesterreichische Vereinigung zur
 * Foerderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.auth.token.repository;

import io.redlink.more.auth.token.model.TokenAuthUserDetails;
import io.redlink.more.auth.model.RoutingInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class TokenAuthUserRepository {

    private static final String GET_AUTH_ROUTING_INFO =
            "SELECT * FROM auth_routing_info WHERE api_id = :api_id";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TokenAuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Optional<TokenAuthUserDetails> findByApiId(String apiId, Set<String> roles) {
        try (var stream = jdbcTemplate.queryForStream(
                GET_AUTH_ROUTING_INFO,
                Map.of("api_id", apiId),
                (rs, rowNum) -> readUserDetails(rs, roles)
        )) {
            return stream.findFirst();
        }
    }

    private static TokenAuthUserDetails readUserDetails(ResultSet rs, Set<String> roles) throws SQLException {
        return new TokenAuthUserDetails(
                rs.getString("api_id"),
                rs.getString("api_secret"),
                roles,
                new RoutingInfo(
                        rs.getLong("study_id"),
                        rs.getInt("participant_id"),
                        DBUtils.readOptionalInt(rs, "study_group_id"),
                        DBUtils.readSet(rs, "observation_group_ids", Integer.class),
                        rs.getBoolean("study_is_active"),
                        true // TODO: This could be read from the db-view, but should always be true
                ));
    }
}
