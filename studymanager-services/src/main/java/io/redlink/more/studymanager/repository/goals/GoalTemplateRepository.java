package io.redlink.more.studymanager.repository.goals;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.model.GoalTemplate;
import io.redlink.more.studymanager.repository.RepositoryUtils;
import io.redlink.more.studymanager.utils.MapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.redlink.more.studymanager.repository.RepositoryUtils.getValidNullableIntegerValue;

@Component
public class GoalTemplateRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GoalTemplateRepository.class);

    private static final String INSERT_NEW_GOAL_TEMPLATE = """
            INSERT INTO goal_templates(study_id,template_id,title,participant_title,participant_info,type,kind,study_group_id,properties)
            VALUES (:study_id,
                    (SELECT COALESCE(MAX(template_id),0)+1 FROM goal_templates WHERE study_id = :study_id),
                    :title,:participant_title,:participant_info,:type,:kind,:study_group_id,:properties::jsonb)""";

    private static final String IMPORT_GOAL_TEMPLATE = """
            INSERT INTO goal_templates(study_id,template_id,title,participant_title,participant_info,type,kind,study_group_id,properties)
            VALUES (:study_id,:template_id,:title,:participant_title,:participant_info,:type,:kind,:study_group_id,:properties::jsonb)""";

    private static final String GET_GOAL_TEMPLATE_BY_IDS = """
            SELECT gt.*,
                   ARRAY_AGG(gtog.observation_group_id) FILTER (WHERE gtog.observation_group_id IS NOT NULL) AS observation_group_ids,
                   ARRAY_AGG(gtt.key) FILTER (WHERE gtt.key IS NOT NULL) AS topic_keys,
                   ARRAY_AGG(gtac.check_id) FILTER (WHERE gtac.check_id IS NOT NULL) AS adherence_check_ids
            FROM goal_templates gt
                LEFT JOIN goal_template_observation_groups gtog ON gt.study_id = gtog.study_id AND gt.template_id = gtog.template_id
                LEFT JOIN goal_template_topics gtt ON gt.study_id = gtt.study_id AND gt.template_id = gtt.template_id
                LEFT JOIN goal_template_adherence_checks gtac ON gt.study_id = gtac.study_id AND gt.template_id = gtac.template_id
            WHERE gt.study_id = ? AND gt.template_id = ?
            GROUP BY gt.study_id, gt.template_id""";

    // FIXED: explicit GROUP BY on every column of goal_templates (this is the root cause of the "size: 0" bug)
    private static final String LIST_GOAL_TEMPLATES = """
            SELECT gt.*,
                   ARRAY_AGG(gtog.observation_group_id) FILTER (WHERE gtog.observation_group_id IS NOT NULL) AS observation_group_ids,
                   ARRAY_AGG(gtt.key) FILTER (WHERE gtt.key IS NOT NULL) AS topic_keys,
                   ARRAY_AGG(gtac.check_id) FILTER (WHERE gtac.check_id IS NOT NULL) AS adherence_check_ids
            FROM goal_templates gt
                LEFT JOIN goal_template_observation_groups gtog ON gt.study_id = gtog.study_id AND gt.template_id = gtog.template_id
                LEFT JOIN goal_template_topics gtt ON gt.study_id = gtt.study_id AND gt.template_id = gtt.template_id
                LEFT JOIN goal_template_adherence_checks gtac ON gt.study_id = gtac.study_id AND gt.template_id = gtac.template_id
            WHERE gt.study_id = :study_id
            GROUP BY gt.study_id, gt.template_id,
                     gt.title, gt.participant_title, gt.participant_info,
                     gt.type, gt.kind, gt.study_group_id,
                     gt.properties, gt.created, gt.modified""";

    // Same fix applied to the "ForGroup" query
    private static final String LIST_GOAL_TEMPLATES_FOR_GROUP = """
            SELECT gt.*,
                   ARRAY_AGG(gtog.observation_group_id) FILTER (WHERE gtog.observation_group_id IS NOT NULL) AS observation_group_ids,
                   ARRAY_AGG(gtt.key) FILTER (WHERE gtt.key IS NOT NULL) AS topic_keys,
                   ARRAY_AGG(gtac.check_id) FILTER (WHERE gtac.check_id IS NOT NULL) AS adherence_check_ids
            FROM goal_templates gt
                LEFT JOIN goal_template_observation_groups gtog ON gt.study_id = gtog.study_id AND gt.template_id = gtog.template_id
                LEFT JOIN goal_template_topics gtt ON gt.study_id = gtt.study_id AND gt.template_id = gtt.template_id
                LEFT JOIN goal_template_adherence_checks gtac ON gt.study_id = gtac.study_id AND gt.template_id = gtac.template_id
            WHERE gt.study_id = :study_id
              AND (gt.study_group_id IS NULL OR gt.study_group_id = :study_group_id)
              AND (NOT EXISTS (SELECT 1 FROM goal_template_observation_groups gtog3 
                               WHERE gtog3.study_id = gt.study_id AND gtog3.template_id = gt.template_id)
                   OR EXISTS (SELECT 1 FROM goal_template_observation_groups gtog2 
                              WHERE gtog2.study_id = gt.study_id 
                                AND gtog2.template_id = gt.template_id 
                                AND gtog2.observation_group_id = ANY(:observation_group_ids)))
            GROUP BY gt.study_id, gt.template_id""";

    private static final String UPDATE_GOAL_TEMPLATE = """
            UPDATE goal_templates 
            SET title=:title, participant_title=:participant_title, participant_info=:participant_info, 
                type=:type, kind=:kind, study_group_id=:study_group_id, properties=:properties::jsonb, modified=now() 
            WHERE study_id=:study_id AND template_id=:template_id""";

    private static final String DELETE_BY_IDS = "DELETE FROM goal_templates WHERE study_id = ? AND template_id = ?";
    private static final String DELETE_ALL = "DELETE FROM goal_templates";

    private static final String DELETE_GOAL_TEMPLATE_OBSERVATION_GROUPS =
            "DELETE FROM goal_template_observation_groups WHERE study_id = :study_id AND template_id = :template_id";

    private static final String SET_GOAL_TEMPLATE_OBSERVATION_GROUPS =
            "INSERT INTO goal_template_observation_groups (study_id, template_id, observation_group_id) " +
                    "SELECT :study_id, :template_id, unnest(:observation_group_ids::int[])";

    private static final String DELETE_GOAL_TEMPLATE_TOPICS =
            "DELETE FROM goal_template_topics WHERE study_id = :study_id AND template_id = :template_id";

    private static final String SET_GOAL_TEMPLATE_TOPICS =
            "INSERT INTO goal_template_topics (study_id, template_id, key) " +
                    "SELECT :study_id, :template_id, unnest(:topic_keys::text[])";

    private static final String DELETE_GOAL_TEMPLATE_ADHERENCE_CHECKS =
            "DELETE FROM goal_template_adherence_checks WHERE study_id = :study_id AND template_id = :template_id";

    private static final String SET_GOAL_TEMPLATE_ADHERENCE_CHECKS =
            "INSERT INTO goal_template_adherence_checks (study_id, template_id, check_id) " +
                    "SELECT :study_id, :template_id, unnest(:adherence_check_ids::int[])";

    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedTemplate;

    public GoalTemplateRepository(JdbcTemplate template) {
        this.template = template;
        this.namedTemplate = new NamedParameterJdbcTemplate(template);
    }

    @Transactional
    public GoalTemplate insert(GoalTemplate goalTemplate) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            namedTemplate.update(INSERT_NEW_GOAL_TEMPLATE, toParams(goalTemplate), keyHolder, new String[]{"template_id"});
        } catch (DataIntegrityViolationException | JsonProcessingException e) {
            LOG.warn("Unable to insert {}", goalTemplate, e);
            throw new BadRequestException("Unable to insert goal template");
        }
        Integer templateId = keyHolder.getKey().intValue();
        setAllMappings(goalTemplate.getStudyId(), templateId, goalTemplate);
        return getById(goalTemplate.getStudyId(), templateId);
    }

    @Transactional
    public GoalTemplate doImport(Long studyId, GoalTemplate goalTemplate) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            namedTemplate.update(IMPORT_GOAL_TEMPLATE,
                    toParams(goalTemplate)
                            .addValue("study_id", studyId)
                            .addValue("template_id", goalTemplate.getTemplateId()),
                    keyHolder, new String[]{"template_id"});
        } catch (DataIntegrityViolationException | JsonProcessingException e) {
            throw new BadRequestException("Error during import of goal template");
        }
        Integer templateId = keyHolder.getKey().intValue();
        setAllMappings(studyId, templateId, goalTemplate);
        return getById(studyId, templateId);
    }

    public GoalTemplate getById(Long studyId, Integer templateId) {
        try {
            return template.queryForObject(GET_GOAL_TEMPLATE_BY_IDS, getGoalTemplateRowMapper(), studyId, templateId);
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("GoalTemplate " + templateId + " or study " + studyId + " does not exist");
        }
    }

    public void deleteGoalTemplate(Long studyId, Integer templateId) {
        template.update(DELETE_BY_IDS, studyId, templateId);
    }

    public List<GoalTemplate> listGoalTemplates(Long studyId) {
        return namedTemplate.query(
                LIST_GOAL_TEMPLATES,
                new MapSqlParameterSource("study_id", studyId),
                getGoalTemplateRowMapper()
        );
    }

    public List<GoalTemplate> listGoalTemplatesForGroup(Long studyId, Integer studyGroupId) {
        return listGoalTemplatesForGroup(studyId, studyGroupId, List.of());
    }

    public List<GoalTemplate> listGoalTemplatesForGroup(Long studyId, Integer studyGroupId, Collection<Integer> observationGroupIds) {
        return namedTemplate.query(
                LIST_GOAL_TEMPLATES_FOR_GROUP,
                new MapSqlParameterSource("study_id", studyId)
                        .addValue("study_group_id", studyGroupId)
                        .addValue("observation_group_ids", observationGroupIds == null || observationGroupIds.isEmpty() ? new Integer[0] : observationGroupIds.toArray(new Integer[0])),
                getGoalTemplateRowMapper()
        );
    }

    @Transactional
    public GoalTemplate update(GoalTemplate goalTemplate) {
        try {
            namedTemplate.update(UPDATE_GOAL_TEMPLATE,
                    toParams(goalTemplate).addValue("template_id", goalTemplate.getTemplateId()));
            setAllMappings(goalTemplate.getStudyId(), goalTemplate.getTemplateId(), goalTemplate);
            return getById(goalTemplate.getStudyId(), goalTemplate.getTemplateId());
        } catch (JsonProcessingException e) {
            LOG.error("Json error while updating goal template", e);
            return null;
        }
    }

    public void clear() {
        template.execute(DELETE_ALL);
    }

    private void setAllMappings(Long studyId, Integer templateId, GoalTemplate goalTemplate) {
        setObservationGroupIds(studyId, templateId, goalTemplate.getObservationGroupIds());
        setTopicKeys(studyId, templateId, goalTemplate.getTopicKeys());
        setAdherenceCheckIds(studyId, templateId, goalTemplate.getAdherenceCheckIds());
    }

    private void setObservationGroupIds(Long studyId, Integer templateId, Set<Integer> ids) {
        final var params = toParams(studyId, templateId);
        namedTemplate.update(DELETE_GOAL_TEMPLATE_OBSERVATION_GROUPS, params);
        if (ids != null && !ids.isEmpty()) {
            params.addValue("observation_group_ids", ids.toArray(new Integer[0]));
            namedTemplate.update(SET_GOAL_TEMPLATE_OBSERVATION_GROUPS, params);
        }
    }

    private void setTopicKeys(Long studyId, Integer templateId, Set<String> keys) {
        final var params = toParams(studyId, templateId);
        namedTemplate.update(DELETE_GOAL_TEMPLATE_TOPICS, params);
        if (keys != null && !keys.isEmpty()) {
            params.addValue("topic_keys", keys.toArray(new String[0]));
            namedTemplate.update(SET_GOAL_TEMPLATE_TOPICS, params);
        }
    }

    private void setAdherenceCheckIds(Long studyId, Integer templateId, Set<Integer> ids) {
        final var params = toParams(studyId, templateId);
        namedTemplate.update(DELETE_GOAL_TEMPLATE_ADHERENCE_CHECKS, params);
        if (ids != null && !ids.isEmpty()) {
            params.addValue("adherence_check_ids", ids.toArray(new Integer[0]));
            namedTemplate.update(SET_GOAL_TEMPLATE_ADHERENCE_CHECKS, params);
        }
    }

    private static MapSqlParameterSource toParams(Long studyId, Integer templateId) {
        return new MapSqlParameterSource()
                .addValue("study_id", studyId)
                .addValue("template_id", templateId);
    }

    private static MapSqlParameterSource toParams(GoalTemplate gt) throws JsonProcessingException {
        return new MapSqlParameterSource()
                .addValue("study_id", gt.getStudyId())
                .addValue("title", gt.getTitle())
                .addValue("participant_title", gt.getParticipantTitle())
                .addValue("participant_info", gt.getParticipantInfo())
                .addValue("type", gt.getType())
                .addValue("kind", gt.getKind())
                .addValue("study_group_id", gt.getStudyGroupId())
                .addValue("properties", MapperUtils.writeValueAsString(gt.getProperties()));
    }

    private static RowMapper<GoalTemplate> getGoalTemplateRowMapper() {
        return (rs, rowNum) -> new GoalTemplate()
                .setStudyId(rs.getLong("study_id"))
                .setTemplateId(rs.getInt("template_id"))
                .setTitle(rs.getString("title"))
                .setParticipantTitle(rs.getString("participant_title"))
                .setParticipantInfo(rs.getString("participant_info"))
                .setType(rs.getString("type"))
                .setKind(rs.getString("kind"))
                .setStudyGroupId(getValidNullableIntegerValue(rs, "study_group_id"))
                .setProperties(MapperUtils.readValue(rs.getString("properties"), GoalTemplateProperties.class))
                .setCreated(RepositoryUtils.readInstant(rs, "created"))
                .setModified(RepositoryUtils.readInstant(rs, "modified"))
                .setObservationGroupIds(RepositoryUtils.readSet(rs, "observation_group_ids", Integer.class))
                .setTopicKeys(RepositoryUtils.readSet(rs, "topic_keys", String.class))
                .setAdherenceCheckIds(RepositoryUtils.readSet(rs, "adherence_check_ids", Integer.class));
    }
}