package io.redlink.more.studymanager.repository.goals;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.redlink.more.studymanager.core.properties.GoalProperties;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.Goal;
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

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Component
public class GoalRepository {

    private final static Logger LOG = LoggerFactory.getLogger(GoalRepository.class);

    private static final String INSERT_NEW_GOAL = """
            INSERT INTO goal(study_id,goal_id,participant_id,template_id,properties)
            VALUES (:study_id,
                    (SELECT COALESCE(MAX(goal_id),0)+1 FROM goal WHERE study_id = :study_id),
                    :participant_id,:template_id,:properties::jsonb)""";

    private static final String GET_GOAL_BY_IDS = "SELECT * FROM goal WHERE study_id = ? AND goal_id = ?";
    private static final String LIST_GOALS = """
            SELECT * FROM goal 
            WHERE study_id = :study_id 
              AND (:participant_id IS NULL OR participant_id = :participant_id)
              AND (:template_id IS NULL OR template_id = :template_id)""";
    private static final String UPDATE_GOAL = """
            UPDATE goal 
            SET participant_id=:participant_id, template_id=:template_id, 
                properties=:properties::jsonb, modified=now() 
            WHERE study_id=:study_id AND goal_id=:goal_id""";
    private static final String DELETE_BY_IDS = "DELETE FROM goal WHERE study_id = ? AND goal_id = ?";
    private static final String DELETE_ALL = "DELETE FROM goal";

    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedTemplate;

    public GoalRepository(JdbcTemplate template) {
        this.template = template;
        this.namedTemplate = new NamedParameterJdbcTemplate(template);
    }

    @Transactional
    public Goal insert(Goal goal) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            namedTemplate.update(INSERT_NEW_GOAL, toParams(goal), keyHolder, new String[]{"goal_id"});
        } catch (DataIntegrityViolationException | JsonProcessingException e) {
            LOG.warn("Unable to insert {}", goal, e);
            throw new BadRequestException("Unable to insert goal");
        }
        Integer goalId = keyHolder.getKey().intValue();
        return getById(goal.getStudyId(), goalId);
    }

    public Goal getById(Long studyId, Integer goalId) {
        try {
            return template.queryForObject(GET_GOAL_BY_IDS, getGoalRowMapper(), studyId, goalId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Goal " + goalId + " or study " + studyId + " does not exist");
        }
    }

    /**
     * Flexible list that treats NULL as wildcard (exactly as requested).
     * Fixed with explicit JDBC types to satisfy PostgreSQL.
     */
    public List<Goal> list(Long studyId, Integer participantId, Integer templateId) {
        return namedTemplate.query(
                LIST_GOALS,
                new MapSqlParameterSource()
                        .addValue("study_id", studyId, Types.BIGINT)
                        .addValue("participant_id", participantId, Types.INTEGER)
                        .addValue("template_id", templateId, Types.INTEGER),
                getGoalRowMapper()
        );
    }

    public void deleteGoal(Long studyId, Integer goalId) {
        template.update(DELETE_BY_IDS, studyId, goalId);
    }

    @Transactional
    public Goal update(Goal goal) {
        try {
            namedTemplate.update(UPDATE_GOAL,
                    toParams(goal).addValue("goal_id", goal.getGoalId()));
            return getById(goal.getStudyId(), goal.getGoalId());
        } catch (JsonProcessingException e) {
            LOG.error("Json error while updating goal", e);
            return null;
        }
    }

    public void clear() {
        template.execute(DELETE_ALL);
    }

    private static MapSqlParameterSource toParams(Goal goal) throws JsonProcessingException {
        return new MapSqlParameterSource()
                .addValue("study_id", goal.getStudyId(), Types.BIGINT)
                .addValue("participant_id", goal.getParticipantId(), Types.INTEGER)
                .addValue("template_id", goal.getTemplateId(), Types.INTEGER)
                .addValue("properties", MapperUtils.writeValueAsString(goal.getProperties()));
    }

    private static RowMapper<Goal> getGoalRowMapper() {
        return (rs, rowNum) -> new Goal()
                .setStudyId(rs.getLong("study_id"))
                .setGoalId(rs.getInt("goal_id"))
                .setParticipantId(rs.getInt("participant_id"))
                .setTemplateId(rs.getInt("template_id"))
                .setProperties(MapperUtils.readValue(rs.getString("properties"), GoalProperties.class))
                .setCreated(RepositoryUtils.readInstant(rs, "created"))
                .setModified(RepositoryUtils.readInstant(rs, "modified"));
    }
}