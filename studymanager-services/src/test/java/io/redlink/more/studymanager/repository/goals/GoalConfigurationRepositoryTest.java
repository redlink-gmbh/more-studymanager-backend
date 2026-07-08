package io.redlink.more.studymanager.repository.goals;

import io.redlink.more.studymanager.configuration.JPAConfiguration;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.DataConstraintException;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.repository.StudyRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        GoalConfigurationRepository.class,
        GoalTemplateRepository.class,
        StudyRepository.class,
        JPAConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles({"test", "test-containers-flyway"})
class GoalConfigurationRepositoryTest {

    @Autowired
    private GoalConfigurationRepository goalConfigurationRepository;

    @Autowired
    private GoalTemplateRepository goalTemplateRepository;

    @Autowired
    private StudyRepository studyRepository;

    @BeforeEach
    void cleanup() {
        // We use separate studies per test → no global cleanup needed
    }

    // =====================================================================
    // STUDY GOAL CONFIG (1:0..1 per study)
    // =====================================================================

    @Test
    @DisplayName("StudyGoalConfig - full CRUD + delete guard when GoalTemplates exist")
    void testStudyGoalConfigCRUD() {
        Long study1 = studyRepository.insert(new Study().setContact(new Contact().setPerson("s1"))).getStudyId();
        Long study2 = studyRepository.insert(new Study().setContact(new Contact().setPerson("s2"))).getStudyId();

        // Create / Update (upsert)
        StudyGoalConfig config1 = new StudyGoalConfig()
                .setStudyId(study1)
                .setCommitment("Commit1")
                .setAchievability("High")
                .setUnderstandability("Medium");

        StudyGoalConfig saved = goalConfigurationRepository.saveStudyGoalConfig(config1);
        assertThat(saved).isNotNull();
        assertThat(saved.getCommitment()).isEqualTo("Commit1");

        // Update existing
        saved.setCommitment("Commit1-updated");
        StudyGoalConfig updated = goalConfigurationRepository.saveStudyGoalConfig(saved);
        assertThat(updated.getCommitment()).isEqualTo("Commit1-updated");

        // Read
        assertThat(goalConfigurationRepository.getStudyGoalConfig(study1))
                .isNotNull()
                .extracting(StudyGoalConfig::getCommitment)
                .isEqualTo("Commit1-updated");

        assertThat(goalConfigurationRepository.getStudyGoalConfig(study2)).isNull(); // other study
        Assertions.setMaxStackTraceElementsDisplayed(100);
        // Delete guard
        Integer t1 = goalTemplateRepository.insert(new GoalTemplate().setStudyId(study1).setType("test")).getTemplateId();
        assertThatThrownBy(() -> goalConfigurationRepository.deleteStudyGoalConfig(study1))
                .isInstanceOf(DataConstraintException.class)
                .hasMessageContaining("Unable to remove");

        // Delete after removing template
        goalTemplateRepository.deleteGoalTemplate(study1, t1);
        goalConfigurationRepository.deleteStudyGoalConfig(study1);
        assertThat(goalConfigurationRepository.getStudyGoalConfig(study1)).isNull();
    }

    // =====================================================================
    // GOAL TOPICS
    // =====================================================================

    @Test
    @DisplayName("GoalTopic - full CRUD + study isolation")
    void testGoalTopicCRUD() {
        Long studyA = studyRepository.insert(new Study().setContact(new Contact().setPerson("A"))).getStudyId();
        Long studyB = studyRepository.insert(new Study().setContact(new Contact().setPerson("B"))).getStudyId();

        // Create multiple topics per study
        GoalTopic tA1 = goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyA).setKey("smoking").setTitle("Smoking Cessation").setDescription("..."));
        GoalTopic tA2 = goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyA).setKey("activity").setTitle("Daily Activity"));

        GoalTopic tB1 = goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyB).setKey("sleep").setTitle("Better Sleep"));

        // List per study
        assertThat(goalConfigurationRepository.listTopics(studyA))
                .hasSize(2)
                .extracting(GoalTopic::getKey)
                .containsExactlyInAnyOrder("smoking", "activity");

        assertThat(goalConfigurationRepository.listTopics(studyB))
                .hasSize(1)
                .extracting(GoalTopic::getKey)
                .containsExactly("sleep");

        // Get single
        assertThat(goalConfigurationRepository.getTopic(studyA, "smoking")).isNotNull();
        assertThat(goalConfigurationRepository.getTopic(studyB, "smoking")).isNull(); // isolation

        // Update (upsert)
        GoalTopic updated = goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyA).setKey("smoking").setTitle("Updated Title"));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");

        // Delete
        goalConfigurationRepository.deleteTopic(studyA, "smoking");
        assertThat(goalConfigurationRepository.listTopics(studyA)).hasSize(1);
    }

    // =====================================================================
    // GOAL ADHERENCE CHECKS
    // =====================================================================

    @Test
    @DisplayName("GoalAdherenceCheck - full CRUD + study isolation")
    void testGoalAdherenceCheckCRUD() {
        Long studyX = studyRepository.insert(new Study().setContact(new Contact().setPerson("X"))).getStudyId();
        Long studyY = studyRepository.insert(new Study().setContact(new Contact().setPerson("Y"))).getStudyId();

        // Create multiple checks per study
        GoalAdherenceCheck cX1 = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck()
                        .setStudyId(studyX)
                        .setCheckId(0)
                        .setTitle("Morning")
                        .setTime(LocalTime.of(8, 0)));
        GoalAdherenceCheck cX2 = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck()
                        .setStudyId(studyX)
                        .setCheckId(4)
                        .setTitle("Evening")
                        .setTime(LocalTime.of(20, 30)));

        GoalAdherenceCheck cY1 = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck()
                        .setStudyId(studyY)
                        .setCheckId(2)
                        .setTitle("Noon")
                        .setTime(LocalTime.of(12, 0)));

        // List per study
        assertThat(goalConfigurationRepository.listChecks(studyX))
                .hasSize(2)
                .extracting(GoalAdherenceCheck::getTitle)
                .containsExactlyInAnyOrder("Morning", "Evening");

        //update check
        cX2.setTime(LocalTime.of(20, 15));
        GoalAdherenceCheck cX2Updated = goalConfigurationRepository.upsertCheck(cX2);
        assertThat(cX2Updated.getTime()).isEqualTo(LocalTime.of(20, 15));

        assertThat(goalConfigurationRepository.listChecks(studyX))
                .hasSize(2)
                .filteredOn(ac -> "Evening".equals(ac.getTitle()))
                .extracting(GoalAdherenceCheck::getTime)
                .containsOnly(cX2Updated.getTime());


        assertThat(goalConfigurationRepository.listChecks(studyY))
                .hasSize(1)
                .extracting(GoalAdherenceCheck::getTitle)
                .containsExactly("Noon");

        // Get by ID
        assertThat(goalConfigurationRepository.getCheckById(studyX, cX1.getCheckId())).isNotNull();
        assertThat(goalConfigurationRepository.getCheckById(studyX, cX2.getCheckId())).isNotNull();
        assertThat(goalConfigurationRepository.getCheckById(studyY, cY1.getCheckId())).isNotNull();
        assertThat(goalConfigurationRepository.getCheckById(studyY, cX2.getCheckId())).isNull(); // isolation

        // Update
        cX1.setTitle("Morning - updated");
        GoalAdherenceCheck updated = goalConfigurationRepository.updateCheck(cX1);
        assertThat(updated.getTitle()).isEqualTo("Morning - updated");

        // Delete
        goalConfigurationRepository.deleteCheck(studyX, cX2.getCheckId());
        assertThat(goalConfigurationRepository.listChecks(studyX)).hasSize(1);
    }


    @Test
    @DisplayName("StudyGoalConfig can be imported (forced studyId)")
    void testDoImportStudyGoalConfig() {
        Long studyA = studyRepository.insert(new Study().setContact(new Contact().setPerson("A"))).getStudyId();
        Long studyB = studyRepository.insert(new Study().setContact(new Contact().setPerson("B"))).getStudyId();

        StudyGoalConfig original = new StudyGoalConfig()
                .setStudyId(studyA)
                .setCommitment("Imported commit")
                .setAchievability("Medium");

        StudyGoalConfig imported = goalConfigurationRepository.doImport(studyB, original);

        assertThat(imported.getStudyId()).isEqualTo(studyB);
        assertThat(imported.getCommitment()).isEqualTo("Imported commit");

        assertThat(goalConfigurationRepository.getStudyGoalConfig(studyB)).isNotNull();
        assertThat(goalConfigurationRepository.getStudyGoalConfig(studyA)).isNull();
    }

    @Test
    @DisplayName("GoalAdherenceCheck can be imported with forced studyId")
    void testDoImportAdherenceCheck() {
        Long studyX = studyRepository.insert(new Study().setContact(new Contact().setPerson("X"))).getStudyId();
        Long studyY = studyRepository.insert(new Study().setContact(new Contact().setPerson("Y"))).getStudyId();

        GoalAdherenceCheck original = new GoalAdherenceCheck()
                .setStudyId(studyX)
                .setCheckId(1)                     // pretend exported
                .setTitle("Imported Check")
                .setTime(LocalTime.of(14, 15));

        GoalAdherenceCheck imported = goalConfigurationRepository.doImport(studyY, original);

        assertThat(imported.getStudyId()).isEqualTo(studyY);
        assertThat(imported.getCheckId()).isEqualTo(1);
        assertThat(imported.getTitle()).isEqualTo("Imported Check");

        assertThat(goalConfigurationRepository.listChecks(studyY)).hasSize(1);
        assertThat(goalConfigurationRepository.listChecks(studyX)).isEmpty();
    }

    @Test
    @DisplayName("GoalAdherenceCheck import does not override existing")
    void testDoSkipImportExistingAdherenceCheck() {
        Long studyX = studyRepository.insert(new Study().setContact(new Contact().setPerson("X"))).getStudyId();
        Long studyY = studyRepository.insert(new Study().setContact(new Contact().setPerson("Y"))).getStudyId();
        //add an existing adherence check for the id=1 to studyY
        goalConfigurationRepository.upsertCheck(new GoalAdherenceCheck()
                .setStudyId(studyY)
                .setCheckId(1)                     // pretend exported
                .setTitle("Existing Check")
                .setTime(LocalTime.of(14, 05)));

        //now create a check for StudyX with the same id=1 but different title and time
        GoalAdherenceCheck originalStudyX = new GoalAdherenceCheck()
                .setStudyId(studyX)
                .setCheckId(1)                     // pretend exported
                .setTitle("Imported Check")
                .setTime(LocalTime.of(14, 15));

        //import the check from studyX and validate that the existing one is not overridden
        GoalAdherenceCheck imported = goalConfigurationRepository.doImport(studyY, originalStudyX);

        assertThat(imported.getStudyId()).isEqualTo(studyY);
        assertThat(imported.getCheckId()).isEqualTo(1);
        assertThat(imported.getTitle()).isEqualTo("Existing Check");
        assertThat(imported.getTime()).isEqualTo(LocalTime.of(14, 05));

        assertThat(goalConfigurationRepository.listChecks(studyY)).hasSize(1);
        assertThat(goalConfigurationRepository.listChecks(studyX)).isEmpty();
    }

    @Test
    @DisplayName("GoalTopic can be imported with forced studyId (upsert behavior)")
    void testDoImportGoalTopic() {
        Long studyA = studyRepository.insert(new Study().setContact(new Contact().setPerson("A"))).getStudyId();
        Long studyB = studyRepository.insert(new Study().setContact(new Contact().setPerson("B"))).getStudyId();

        // Original topic pretends to come from studyA
        GoalTopic original = new GoalTopic()
                .setStudyId(studyA)
                .setKey("hydration")
                .setTitle("Drink Water")
                .setDescription("Stay hydrated daily");

        // Import into studyB → should use studyB as study_id
        GoalTopic imported = goalConfigurationRepository.doImport(studyB, original);

        assertThat(imported.getStudyId()).isEqualTo(studyB);
        assertThat(imported.getKey()).isEqualTo("hydration");
        assertThat(imported.getTitle()).isEqualTo("Drink Water");

        // Verify it's visible under studyB
        assertThat(goalConfigurationRepository.listTopics(studyB))
                .hasSize(1)
                .extracting(GoalTopic::getKey)
                .containsExactly("hydration");

        // Verify it did NOT appear in studyA
        assertThat(goalConfigurationRepository.listTopics(studyA)).isEmpty();

        // Update via import (same key → should overwrite)
        GoalTopic updatedVersion = new GoalTopic()
                .setStudyId(studyA)           // this should be ignored
                .setKey("hydration")
                .setTitle("Updated - Drink More Water");

        GoalTopic reImported = goalConfigurationRepository.doImport(studyB, updatedVersion);

        assertThat(reImported.getStudyId()).isEqualTo(studyB);
        assertThat(reImported.getTitle()).isEqualTo("Updated - Drink More Water");

        assertThat(goalConfigurationRepository.getTopic(studyB, "hydration").getTitle())
                .isEqualTo("Updated - Drink More Water");
    }

    @Test
    @DisplayName("deleteTopic throws DataConstraintException when topic is used by a GoalTemplate")
    void testDeleteTopic_ThrowsWhenUsed() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test"))).getStudyId();

        String topicKey = "exercise";
        // Create topic
        goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyId).setKey(topicKey).setTitle("Physical Exercise")
        );

        // Create a GoalTemplate that uses this topic
        GoalTemplate template = new GoalTemplate()
                .setStudyId(studyId)
                .setTitle("Template using exercise topic")
                .setType("fitness")
                .setTopicKeys(Set.of(topicKey));

        goalTemplateRepository.insert(template);

        // Attempt to delete the used topic → should fail with DataConstraintException
        assertThatThrownBy(() -> goalConfigurationRepository.deleteTopic(studyId, topicKey))
                .isInstanceOf(DataConstraintException.class)
                .hasMessageContaining("Unable to remove Goal Topic %s from study_%s".formatted(topicKey, studyId))
                .hasMessageContaining("This Topic is still referenced by GoalTemplates in the Study!");
    }

    @Test
    @DisplayName("deleteCheck throws DataConstraintException when check is used by a GoalTemplate")
    void testDeleteCheck_ThrowsWhenUsed() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test"))).getStudyId();

        // Create adherence check
        GoalAdherenceCheck check = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck()
                        .setStudyId(studyId)
                        .setCheckId(1)
                        .setTitle("Morning")
                        .setTime(LocalTime.of(20, 0))
        );

        // Create a GoalTemplate that uses this check
        GoalTemplate template = new GoalTemplate()
                .setStudyId(studyId)
                .setTitle("Template using morning check")
                .setType("habit")
                .setAdherenceCheckIds(Set.of(check.getCheckId()));

        goalTemplateRepository.insert(template);

        // Attempt to delete the used check → should fail
        assertThatThrownBy(() -> goalConfigurationRepository.deleteCheck(studyId, check.getCheckId()))
                .isInstanceOf(DataConstraintException.class)
                .hasMessageContaining("Unable to remove Adherence Check %s from study_%s".formatted(check.getCheckId(), studyId))
                .hasMessageContaining("This Adherence Check is still referenced by GoalTemplates in the Study");
    }

    @Test
    @DisplayName("deleteTopic and deleteCheck work when not referenced")
    void testDeleteTopicAndCheck_WhenNotUsed() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test"))).getStudyId();

        // Create unused topic and check
        goalConfigurationRepository.saveTopic(
                new GoalTopic().setStudyId(studyId).setKey("unused-topic").setTitle("Unused")
        );

        GoalAdherenceCheck check = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck()
                        .setStudyId(studyId)
                        .setCheckId(1)
                        .setTitle("Morning")
                        .setTime(LocalTime.of(10, 0))
        );

        // These should succeed
        goalConfigurationRepository.deleteTopic(studyId, "unused-topic");
        goalConfigurationRepository.deleteCheck(studyId, check.getCheckId());

        assertThat(goalConfigurationRepository.listTopics(studyId)).isEmpty();
        assertThat(goalConfigurationRepository.listChecks(studyId)).isEmpty();
    }

}