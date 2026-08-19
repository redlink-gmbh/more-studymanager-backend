/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.model.StudyComponent;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StudyComponentRepository {

    private static final Map<String, String> COMPONENT_QUERIES = Map.of(
            "studygroup", """
                SELECT 'studygroup' as type, study_id, study_group_id as id, title
                FROM study_groups
                """,

            "observation", """
                SELECT 'observation' as type, study_id, observation_id as id, title
                FROM observations
                """,

            "intervention", """
                SELECT 'intervention' as type, study_id, intervention_id as id, title
                FROM interventions
                """,

            "participant", """
                SELECT 'participant' as type, study_id, participant_id as id, alias as title
                FROM participants
                """,

            "observationgroup", """
                SELECT 'observationgroup' as type, study_id, observation_group_id as id, title
                FROM observation_groups
                """,

            "goaltemplate", """
                SELECT 'goaltemplate' as type, study_id, template_id as id, title
                FROM goal_templates
                """,

            "goal", """
                SELECT 'goal' as type, g.study_id, g.goal_id as id, gt.title
                FROM goal g
                JOIN goal_templates gt ON g.study_id = gt.study_id AND g.template_id = gt.template_id
                """
    );

    private static final String ALL_TYPES_SQL = buildAllTypesSql();

    private final JdbcTemplate template;

    public StudyComponentRepository(JdbcTemplate template) {
        this.template = template;
    }

    public Map<String, Map<Integer, StudyComponent>> getComponents(Long studyId) {
        return getComponents(studyId, (Set<String>) null);
    }

    public Map<String, Map<Integer, StudyComponent>> getComponents(Long studyId, Set<String> includedComponentTypes) {
        String baseSql = buildSqlForTypes(includedComponentTypes);
        String sql = """
                SELECT * FROM (
                    %s
                ) AS components
                WHERE study_id = ?
                """.formatted(baseSql);

        List<StudyComponent> components;
        if (includedComponentTypes != null && !includedComponentTypes.isEmpty()) {
            sql += " AND type = ANY(?)";
            components = template.query(sql, getStudyComponentRowMapper(), studyId, includedComponentTypes.toArray(new String[0]));
        } else {
            components = template.query(sql, getStudyComponentRowMapper(), studyId);
        }

        Map<String, Map<Integer, StudyComponent>> result = new HashMap<>();
        for (StudyComponent sc : components) {
            result.computeIfAbsent(sc.type(), k -> new HashMap<>())
                    .put(sc.componentId(), sc);
        }
        return result;
    }

    public Map<Integer, StudyComponent> getComponents(Long studyId, String type) {
        String fragment = COMPONENT_QUERIES.get(type);
        if (fragment == null) {
            return Map.of();
        }

        String sql = """
                SELECT * FROM (
                    %s
                ) AS components
                WHERE study_id = ?
                """.formatted(fragment);

        List<StudyComponent> list = template.query(sql, getStudyComponentRowMapper(), studyId);

        Map<Integer, StudyComponent> map = new HashMap<>();
        for (StudyComponent sc : list) {
            map.put(sc.componentId(), sc);
        }
        return map;
    }

    public StudyComponent getComponent(Long studyId, String type, Integer id) {
        String fragment = COMPONENT_QUERIES.get(type);
        if (fragment == null) {
            return null;
        }

        String sql = """
                SELECT * FROM (
                    %s
                ) AS components
                WHERE study_id = ? AND id = ?
                """.formatted(fragment);

        try {
            return template.queryForObject(sql, getStudyComponentRowMapper(), studyId, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static RowMapper<StudyComponent> getStudyComponentRowMapper() {
        return (rs, rowNum) -> new StudyComponent(
                rs.getLong("study_id"),
                rs.getInt("id"),
                rs.getString("type"),
                rs.getString("title")
        );
    }

    private static String buildAllTypesSql() {
        return COMPONENT_QUERIES.values().stream()
                .collect(Collectors.joining(" UNION ALL\n"));
    }

    private String buildSqlForTypes(Set<String> types) {
        if (types == null || types.isEmpty()) {
            return ALL_TYPES_SQL;
        }

        return types.stream()
                .filter(COMPONENT_QUERIES::containsKey)
                .map(COMPONENT_QUERIES::get)
                .collect(Collectors.joining(" UNION ALL\n"));
    }
}