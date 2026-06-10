package io.redlink.more.studymanager.repository.goals;

import io.redlink.more.studymanager.configuration.JPAConfiguration;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.repository.ObservationGroupRepository;
import io.redlink.more.studymanager.repository.StudyGroupRepository;
import io.redlink.more.studymanager.repository.StudyRepository;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        GoalTemplateRepository.class,
        GoalConfigurationRepository.class,
        StudyRepository.class,
        StudyGroupRepository.class,
        ObservationGroupRepository.class,
        JPAConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles({"test", "test-containers-flyway"})
class GoalTemplateRepositoryTest {

    @Autowired
    private GoalTemplateRepository goalTemplateRepository;

    @Autowired
    private GoalConfigurationRepository goalConfigurationRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private ObservationGroupRepository observationGroupRepository;

    @BeforeEach
    void deleteAll() {
        goalTemplateRepository.clear();
    }

    @Test
    @DisplayName("GoalTemplates are inserted, updated, listed and the listForGroup semantics work correctly (mirrors ObservationRepositoryTest)")
    public void testInsertListUpdateDeleteAndListForGroupSemantics() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup().setStudyId(studyId)).getStudyGroupId();

        Integer obsGroupId1 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("OG1")).getObservationGroupId();
        Integer obsGroupId2 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("OG2")).getObservationGroupId();

        // === Create prerequisites (required by FK constraints) ===
        goalConfigurationRepository.saveTopic(new GoalTopic().setStudyId(studyId).setKey("health").setTitle("Health"));
        goalConfigurationRepository.saveTopic(new GoalTopic().setStudyId(studyId).setKey("lifestyle").setTitle("Lifestyle"));

        GoalAdherenceCheck morningCheck = goalConfigurationRepository.upsertCheck(
                new GoalAdherenceCheck().setStudyId(studyId)
                        //NOTE: CheckId is expected to be set on the ordinal of an enum, title to the name
                        .setCheckId(1).setTitle("Morning")
                        .setTime(LocalTime.of(8, 0))
        );

        // === Create GoalTemplates with all combinations (exactly like the Observation test) ===
        GoalTemplate gt1 = new GoalTemplate()                                      // studyGroup + obsGroup1
                .setStudyId(studyId)
                .setTitle("GT1 - studyGroup + OG1")
                .setType("smoking")
                .setStudyGroupId(studyGroupId)
                .setProperties(new GoalTemplateProperties(Map.of("target", 10)))
                .setObservationGroupIds(Set.of(obsGroupId1))
                .setTopicKeys(Set.of("health"))
                .setAdherenceCheckIds(Set.of(morningCheck.getCheckId()));

        GoalTemplate gt2 = new GoalTemplate()                                      // global, NO obs group
                .setStudyId(studyId)
                .setTitle("GT2 - global (no obs group)")
                .setType("activity")
                .setProperties(new GoalTemplateProperties(Map.of("target", 5000)))
                .setTopicKeys(Set.of("lifestyle"))
                .setAdherenceCheckIds(Set.of(morningCheck.getCheckId()));

        GoalTemplate gt3a = new GoalTemplate()                                     // global + obsGroup1
                .setStudyId(studyId)
                .setTitle("GT3a - global + OG1")
                .setType("nutrition")
                .setProperties(new GoalTemplateProperties(Map.of("target", 3)))
                .setObservationGroupIds(Set.of(obsGroupId1))
                .setTopicKeys(Set.of("health"))
                .setAdherenceCheckIds(Set.of(morningCheck.getCheckId()));

        GoalTemplate gt3b = new GoalTemplate()                                     // global + obsGroup2
                .setStudyId(studyId)
                .setTitle("GT3b - global + OG2")
                .setType("sleep")
                .setProperties(new GoalTemplateProperties(Map.of("target", 8)))
                .setObservationGroupIds(Set.of(obsGroupId2))
                .setTopicKeys(Set.of("lifestyle"))
                .setAdherenceCheckIds(Set.of(morningCheck.getCheckId()));

        GoalTemplate gt3c = new GoalTemplate()                                     // global + both obs groups
                .setStudyId(studyId)
                .setTitle("GT3c - global + OG1 & OG2")
                .setType("mindfulness")
                .setProperties(new GoalTemplateProperties(Map.of("target", 15)))
                .setObservationGroupIds(Set.of(obsGroupId1, obsGroupId2))
                .setTopicKeys(Set.of("health", "lifestyle"))
                .setAdherenceCheckIds(Set.of(morningCheck.getCheckId()));

        // Insert all
        gt1 = goalTemplateRepository.insert(gt1);
        gt2 = goalTemplateRepository.insert(gt2);
        gt3a = goalTemplateRepository.insert(gt3a);
        gt3b = goalTemplateRepository.insert(gt3b);
        gt3c = goalTemplateRepository.insert(gt3c);

        assertThat(goalTemplateRepository.listGoalTemplates(studyId))
                .as("List all GoalTemplates")
                .hasSize(5);

        // === Test listGoalTemplatesForGroup semantics (exact same cases as in ObservationRepositoryTest) ===

        assertThat(goalTemplateRepository.listGoalTemplatesForGroup(studyId, studyGroupId))
                .as("Only globals that have NO observation group at all (group-specific templates are only returned when matching observationGroupIds are passed)")
                .hasSize(1)
                .extracting(GoalTemplate::getTitle)
                .containsExactly("GT2 - global (no obs group)");

        assertThat(goalTemplateRepository.listGoalTemplatesForGroup(studyId, -1))
                .as("Non-existing Group should only retrieve 'global' templates with no observation group")
                .hasSize(1)
                .extracting(GoalTemplate::getTitle)
                .containsExactly("GT2 - global (no obs group)");

        assertThat(goalTemplateRepository.listGoalTemplatesForGroup(studyId, null))
                .as("<null>-Group should only retrieve 'global' templates with no observation group")
                .hasSize(1)
                .extracting(GoalTemplate::getTitle)
                .containsExactly("GT2 - global (no obs group)");

        // With explicit observation group filter (these are the cases you confirmed work)
        assertThat(goalTemplateRepository.listGoalTemplatesForGroup(studyId, null, Set.of(obsGroupId1)))
                .as("Global context + obsGroup1 (includes globals with no obs group + matching obs group)")
                .hasSize(3)
                .extracting(GoalTemplate::getTitle)
                .containsExactlyInAnyOrder("GT2 - global (no obs group)", "GT3a - global + OG1", "GT3c - global + OG1 & OG2");

        assertThat(goalTemplateRepository.listGoalTemplatesForGroup(studyId, -1, Set.of(obsGroupId1, obsGroupId2)))
                .as("Non-existing group + multiple obs groups")
                .hasSize(4)
                .extracting(GoalTemplate::getTitle)
                .containsExactlyInAnyOrder("GT2 - global (no obs group)", "GT3a - global + OG1", "GT3b - global + OG2", "GT3c - global + OG1 & OG2");

        // Test update + delete
        gt1.setTitle("Updated GT1").setObservationGroupIds(null);
        GoalTemplate updated = goalTemplateRepository.update(gt1);
        assertThat(updated.getTitle()).isEqualTo("Updated GT1");
        assertThat(updated.getObservationGroupIds()).isEmpty();

        goalTemplateRepository.deleteGoalTemplate(studyId, gt3c.getTemplateId());
        assertThat(goalTemplateRepository.listGoalTemplates(studyId)).hasSize(4);
    }

    @Test
    @DisplayName("GoalTemplate can be imported with forced studyId")
    void testDoImportUsesForcedStudyId() {
        Long sourceStudyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("source"))).getStudyId();
        Long targetStudyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("target"))).getStudyId();

        GoalTemplate original = new GoalTemplate()
                .setStudyId(sourceStudyId)
                .setTemplateId(7)  // pretend it came from export
                .setTitle("Imported Goal")
                .setType("weight")
                .setProperties(new GoalTemplateProperties(Map.of("unit", "kg")));

        GoalTemplate imported = goalTemplateRepository.doImport(targetStudyId, original);

        assertThat(imported.getStudyId()).isEqualTo(targetStudyId);
        assertThat(imported.getTemplateId()).isEqualTo(7);
        assertThat(imported.getTitle()).isEqualTo("Imported Goal");

        // verify it is NOT visible under the original study id
        assertThat(goalTemplateRepository.listGoalTemplates(sourceStudyId)).isEmpty();
        assertThat(goalTemplateRepository.listGoalTemplates(targetStudyId)).hasSize(1);
    }

}