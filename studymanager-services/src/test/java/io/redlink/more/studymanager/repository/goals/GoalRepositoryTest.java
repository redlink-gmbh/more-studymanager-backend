package io.redlink.more.studymanager.repository.goals;

import io.redlink.more.studymanager.configuration.JPAConfiguration;
import io.redlink.more.studymanager.core.properties.GoalProperties;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.repository.goals.GoalTemplateRepository;
import io.redlink.more.studymanager.repository.ParticipantRepository;
import io.redlink.more.studymanager.repository.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        GoalRepository.class, GoalTemplateRepository.class, StudyRepository.class,
        ParticipantRepository.class, JPAConfiguration.class
})
@ActiveProfiles("test-containers-flyway")
class GoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalTemplateRepository goalTemplateRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @BeforeEach
    void deleteAll() {
        goalRepository.clear();
    }

    @Test
    public void testInsertListUpdateDeleteAndFlexibleQueries() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Integer participantId = participantRepository.insert(new Participant().setStudyId(studyId).setRegistrationToken("t")).getParticipantId();
        Integer templateId = goalTemplateRepository.insert(new GoalTemplate().setStudyId(studyId).setType("test")).getTemplateId();

        GoalProperties props = new GoalProperties(Map.of("progress", 50));

        Goal goal = new Goal()
                .setStudyId(studyId)
                .setParticipantId(participantId)
                .setTemplateId(templateId)
                .setProperties(props);

        Goal inserted = goalRepository.insert(goal);
        assertThat(inserted.getGoalId()).isNotNull();
        assertThat(inserted.getProperties().getInt("progress")).isEqualTo(50);

        assertThat(goalRepository.list(studyId, null, null)).hasSize(1);
        assertThat(goalRepository.list(studyId, participantId, null)).hasSize(1);
        assertThat(goalRepository.list(studyId, null, templateId)).hasSize(1);
        assertThat(goalRepository.list(studyId, participantId, templateId)).hasSize(1);

        inserted.setProperties(new GoalProperties(Map.of("progress", 75)));
        Goal updated = goalRepository.update(inserted);
        assertThat(updated.getProperties().getInt("progress")).isEqualTo(75);

        goalRepository.deleteGoal(studyId, inserted.getGoalId());
        assertThat(goalRepository.list(studyId, null, null)).isEmpty();
    }

    @Test
    public void testFlexibleListQueriesWithMultipleGoalsPerParticipantAndTemplate() {
        // === Setup ===
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();

        Integer p1 = participantRepository.insert(new Participant().setStudyId(studyId).setRegistrationToken("p1")).getParticipantId();
        Integer p2 = participantRepository.insert(new Participant().setStudyId(studyId).setRegistrationToken("p2")).getParticipantId();

        Integer t1 = goalTemplateRepository.insert(new GoalTemplate().setStudyId(studyId).setType("smoking")).getTemplateId();
        Integer t2 = goalTemplateRepository.insert(new GoalTemplate().setStudyId(studyId).setType("activity")).getTemplateId();

        // === Create goals — including the special case: one participant has TWO goals for the SAME template ===
        Goal gP1T1a = goalRepository.insert(new Goal()
                .setStudyId(studyId)
                .setParticipantId(p1)
                .setTemplateId(t1)
                .setProperties(new GoalProperties(Map.of("progress", 30))));

        Goal gP1T1b = goalRepository.insert(new Goal()
                .setStudyId(studyId)
                .setParticipantId(p1)
                .setTemplateId(t1)           // ← same template as gP1T1a
                .setProperties(new GoalProperties(Map.of("progress", 60))));

        Goal gP1T2 = goalRepository.insert(new Goal()
                .setStudyId(studyId)
                .setParticipantId(p1)
                .setTemplateId(t2)
                .setProperties(new GoalProperties(Map.of("progress", 100))));

        Goal gP2T1 = goalRepository.insert(new Goal()
                .setStudyId(studyId)
                .setParticipantId(p2)
                .setTemplateId(t1)
                .setProperties(new GoalProperties(Map.of("progress", 10))));

        Goal gP2T2 = goalRepository.insert(new Goal()
                .setStudyId(studyId)
                .setParticipantId(p2)
                .setTemplateId(t2)
                .setProperties(new GoalProperties(Map.of("progress", 80))));

        // === Full coverage of list(studyId, participantId, templateId) — NULL = wildcard ===

        // 1. All goals in the study
        assertThat(goalRepository.list(studyId, null, null))
                .as("All goals in study")
                .hasSize(5);

        // 2. Specific participant, any template
        assertThat(goalRepository.list(studyId, p1, null))
                .as("All goals of participant 1")
                .hasSize(3)
                .extracting(g -> g.getParticipantId() + "-" + g.getTemplateId())
                .containsExactlyInAnyOrder(p1 + "-" + t1, p1 + "-" + t1, p1 + "-" + t2);

        assertThat(goalRepository.list(studyId, p2, null))
                .as("All goals of participant 2")
                .hasSize(2)
                .extracting(g -> g.getParticipantId() + "-" + g.getTemplateId())
                .containsExactlyInAnyOrder(p2 + "-" + t1, p2 + "-" + t2);

        // 3. Any participant, specific template
        assertThat(goalRepository.list(studyId, null, t1))
                .as("All goals using template 1 (across participants)")
                .hasSize(3)
                .extracting(g -> g.getParticipantId() + "-" + g.getTemplateId())
                .containsExactlyInAnyOrder(p1 + "-" + t1, p1 + "-" + t1, p2 + "-" + t1);

        assertThat(goalRepository.list(studyId, null, t2))
                .as("All goals using template 2")
                .hasSize(2)
                .extracting(g -> g.getParticipantId() + "-" + g.getTemplateId())
                .containsExactlyInAnyOrder(p1 + "-" + t2, p2 + "-" + t2);

        // 4. Specific participant + specific template (the most important case)
        assertThat(goalRepository.list(studyId, p1, t1))
                .as("Participant 1 + Template 1 → TWO goals")
                .hasSize(2)
                .extracting(Goal::getGoalId)
                .containsExactlyInAnyOrder(gP1T1a.getGoalId(), gP1T1b.getGoalId());

        assertThat(goalRepository.list(studyId, p1, t2))
                .as("Participant 1 + Template 2")
                .hasSize(1);

        assertThat(goalRepository.list(studyId, p2, t1))
                .as("Participant 2 + Template 1")
                .hasSize(1);

        assertThat(goalRepository.list(studyId, p2, t2))
                .as("Participant 2 + Template 2")
                .hasSize(1);

        // === Update & Delete (kept from previous version) ===
        gP1T1a.setProperties(new GoalProperties(Map.of("progress", 75)));
        Goal updated = goalRepository.update(gP1T1a);
        assertThat(updated.getProperties().getInt("progress")).isEqualTo(75);

        goalRepository.deleteGoal(studyId, gP1T1b.getGoalId());
        assertThat(goalRepository.list(studyId, null, null)).hasSize(4);
    }
}