package io.redlink.more.studymanager.repository.goals;

import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.DataConstraintException;
import io.redlink.more.studymanager.model.GoalAdherenceCheck;
import io.redlink.more.studymanager.model.GoalTopic;
import io.redlink.more.studymanager.model.StudyGoalConfig;
import io.redlink.more.studymanager.repository.RepositoryUtils;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Component
public class GoalConfigurationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GoalConfigurationRepository.class);

    private static final String GET_STUDY_GOAL_CONFIG = "SELECT * FROM study_goal_config WHERE study_id = ?";
    private static final String UPSERT_STUDY_GOAL_CONFIG = """
            INSERT INTO study_goal_config(study_id, commitment, achievability, understandability)
            VALUES (:study_id, :commitment, :achievability, :understandability)
            ON CONFLICT (study_id) DO UPDATE SET 
                commitment = EXCLUDED.commitment,
                achievability = EXCLUDED.achievability,
                understandability = EXCLUDED.understandability""";
    private static final String DELETE_STUDY_GOAL_CONFIG = "DELETE FROM study_goal_config WHERE study_id = ?";
    private static final String COUNT_GOAL_TEMPLATES = "SELECT COUNT(*) FROM goal_templates WHERE study_id = ?";

    private static final String INSERT_OR_UPDATE_TOPIC = """
            INSERT INTO goal_topics(study_id, key, title, description)
            VALUES (:study_id, :key, :title, :description)
            ON CONFLICT (study_id, key) DO UPDATE SET
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                modified = now()""";
    private static final String GET_TOPIC = "SELECT * FROM goal_topics WHERE study_id = ? AND key = ?";
    private static final String LIST_TOPICS = "SELECT * FROM goal_topics WHERE study_id = ? ORDER BY key";
    private static final String DELETE_TOPIC = "DELETE FROM goal_topics WHERE study_id = ? AND key = ?";

    private static final String UPSERT_ADHERENCE_CHECK = """
        INSERT INTO goal_adherence_checks (study_id, check_id, title, time)
        VALUES (:study_id, :check_id, :title, :time)
        ON CONFLICT (study_id, check_id) DO UPDATE
            SET title = EXCLUDED.title,
                time = EXCLUDED.time;""";
    private static final String IMPORT_ADHERENCE_CHECK = """
            INSERT INTO goal_adherence_checks(study_id, check_id, title, time)
            VALUES (:study_id, :check_id, :title, :time)
            """;

    private static final String GET_ADHERENCE_CHECK_BY_ID = "SELECT * FROM goal_adherence_checks WHERE study_id = ? AND check_id = ?";
    private static final String LIST_ADHERENCE_CHECKS = "SELECT * FROM goal_adherence_checks WHERE study_id = ? ORDER BY check_id";
    private static final String DELETE_ADHERENCE_CHECK = "DELETE FROM goal_adherence_checks WHERE study_id = ? AND check_id = ?";
    private static final String DELETE_ADHERENCE_CHECKS = "DELETE FROM goal_adherence_checks WHERE study_id = ?";
    private static final String UPDATE_ADHERENCE_CHECK = "UPDATE goal_adherence_checks SET title = :title, time = :time WHERE study_id = :study_id AND check_id = :check_id";

    private static final String DELETE_ALL = "DELETE FROM study_goal_config";


    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedTemplate;

    public GoalConfigurationRepository(JdbcTemplate template) {
        this.template = template;
        this.namedTemplate = new NamedParameterJdbcTemplate(template);
    }

    public StudyGoalConfig getStudyGoalConfig(Long studyId) {
        try {
            return template.queryForObject(GET_STUDY_GOAL_CONFIG, getStudyGoalConfigRowMapper(), studyId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional
    public StudyGoalConfig saveStudyGoalConfig(StudyGoalConfig config) {
        namedTemplate.update(UPSERT_STUDY_GOAL_CONFIG, toParams(config));
        return getStudyGoalConfig(config.getStudyId());
    }

    @Transactional
    public StudyGoalConfig doImport(Long studyId, StudyGoalConfig config) {
        // force studyId – ignore config.getStudyId()
        config.setStudyId(studyId);
        return saveStudyGoalConfig(config);
    }

    @Transactional
    public void deleteStudyGoalConfig(Long studyId) {
        Integer count = template.queryForObject(COUNT_GOAL_TEMPLATES, Integer.class, studyId);
        if (count != null && count > 0) {
            throw DataConstraintException.createWithMessage(studyId, "Gaol Configuration", "Cannot delete study goal config while GoalTemplates exist for study " + studyId);
        }
        template.update(DELETE_STUDY_GOAL_CONFIG, studyId);
    }

    public GoalTopic saveTopic(GoalTopic topic) {
        namedTemplate.update(INSERT_OR_UPDATE_TOPIC, toParams(topic));
        return getTopic(topic.getStudyId(), topic.getKey());
    }

    @Transactional
    public GoalTopic doImport(Long studyId, GoalTopic topic) {
        var existing = getTopic(studyId, topic.getKey());
        if(existing == null) {
            // Force the correct studyId — ignore whatever came in the object
            topic.setStudyId(studyId);
            return saveTopic(topic);
        } else {
            return existing;
        }
    }

    public GoalTopic getTopic(Long studyId, String key) {
        try {
            return template.queryForObject(GET_TOPIC, getGoalTopicRowMapper(), studyId, key);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<GoalTopic> listTopics(Long studyId) {
        return template.query(LIST_TOPICS, getGoalTopicRowMapper(), studyId);
    }

    public void deleteTopic(Long studyId, String key) {
        try {
            template.update(DELETE_TOPIC, studyId, key);
        } catch (DataIntegrityViolationException e) {
            throw DataConstraintException.createWithMessage(studyId, "Goal Topic " + key, "This Topic is still referenced by GoalTemplates in the Study!");
        }
    }

    @Transactional
    public GoalAdherenceCheck upsertCheck(GoalAdherenceCheck check) {
        namedTemplate.update(UPSERT_ADHERENCE_CHECK, toParams(check));
        return getCheckById(check.getStudyId(), check.getCheckId());
    }

    @Transactional
    public GoalAdherenceCheck doImport(Long studyId, GoalAdherenceCheck check) {
        var existing = getCheckById(studyId, check.getCheckId());
        if(existing == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedTemplate.update(
                IMPORT_ADHERENCE_CHECK,
                toParams(check)
                    .addValue("study_id", studyId),
                keyHolder,
                new String[]{"check_id"}
            );
            Integer checkId = Objects.requireNonNull(keyHolder.getKey()).intValue();
            return getCheckById(studyId, checkId);
        } else { //do not override existing adherence check in the study
            return existing;
        }
    }

    /**
     * The adherence check or NULL if not present
     * @param studyId
     * @param checkId
     * @return
     */
    public GoalAdherenceCheck getCheckById(Long studyId, Integer checkId) {
        try {
            return template.queryForObject(GET_ADHERENCE_CHECK_BY_ID, getGoalAdherenceCheckRowMapper(), studyId, checkId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<GoalAdherenceCheck> listChecks(Long studyId) {
        return template.query(LIST_ADHERENCE_CHECKS, getGoalAdherenceCheckRowMapper(), studyId);
    }

    public GoalAdherenceCheck updateCheck(GoalAdherenceCheck check) {
        namedTemplate.update(UPDATE_ADHERENCE_CHECK, toParams(check).addValue("check_id", check.getCheckId()));
        return getCheckById(check.getStudyId(), check.getCheckId());
    }

    public void deleteChecks(Long studyId) {
        try {
            template.update(DELETE_ADHERENCE_CHECKS, studyId);
        } catch (DataIntegrityViolationException e) {
            throw DataConstraintException.createWithMessage(studyId, "Adherence Checks", "Adherence Checks are still referenced by GoalTemplates in the Study!");
        }
    }

    public void deleteCheck(Long studyId, Integer checkId) {
        try {
            template.update(DELETE_ADHERENCE_CHECK, studyId, checkId);
        } catch (DataIntegrityViolationException e) {
            throw DataConstraintException.createWithMessage(studyId, "Adherence Check " + checkId, "This Adherence Check is still referenced by GoalTemplates in the Study!");
        }
    }

    public void clear() {
        template.execute(DELETE_ALL);
    }

    private MapSqlParameterSource toParams(StudyGoalConfig config) {
        return new MapSqlParameterSource()
                .addValue("study_id", config.getStudyId())
                .addValue("commitment", config.getCommitment())
                .addValue("achievability", config.getAchievability())
                .addValue("understandability", config.getUnderstandability());
    }

    private MapSqlParameterSource toParams(GoalTopic topic) {
        return new MapSqlParameterSource()
                .addValue("study_id", topic.getStudyId())
                .addValue("key", topic.getKey())
                .addValue("title", topic.getTitle())
                .addValue("description", topic.getDescription());
    }

    private MapSqlParameterSource toParams(GoalAdherenceCheck check) {
        return new MapSqlParameterSource()
                .addValue("study_id", check.getStudyId())
                .addValue("check_id", check.getCheckId())
                .addValue("title", check.getTitle())
                .addValue("time", check.getTime());
    }

    private static RowMapper<StudyGoalConfig> getStudyGoalConfigRowMapper() {
        return (rs, rowNum) -> new StudyGoalConfig()
                .setStudyId(rs.getLong("study_id"))
                .setCommitment(rs.getString("commitment"))
                .setAchievability(rs.getString("achievability"))
                .setUnderstandability(rs.getString("understandability"));
    }

    private static RowMapper<GoalTopic> getGoalTopicRowMapper() {
        return (rs, rowNum) -> new GoalTopic()
                .setStudyId(rs.getLong("study_id"))
                .setKey(rs.getString("key"))
                .setTitle(rs.getString("title"))
                .setDescription(rs.getString("description"))
                .setCreated(RepositoryUtils.readInstant(rs, "created"))
                .setModified(RepositoryUtils.readInstant(rs, "modified"));
    }

    private static RowMapper<GoalAdherenceCheck> getGoalAdherenceCheckRowMapper() {
        return (rs, rowNum) -> new GoalAdherenceCheck()
                .setStudyId(rs.getLong("study_id"))
                .setCheckId(rs.getInt("check_id"))
                .setTitle(rs.getString("title"))
                .setTime(rs.getObject("time", LocalTime.class));
    }
}
