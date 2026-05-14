/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.controller.studymanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.redlink.more.studymanager.api.v1.model.*;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.model.transformer.GoalV1Transformer;
import io.redlink.more.studymanager.service.GoalService;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import io.redlink.more.studymanager.service.StudyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({GoalsApiV1Controller.class})
@AutoConfigureMockMvc(addFilters = false)
class GoalsApiV1ControllerTest {

    @MockitoBean
    StudyService studyService;

    @MockitoBean
    GoalService goalService;

    @MockitoBean
    OAuth2AuthenticationService oAuth2AuthenticationService;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private MockMvc mvc;

    private static final Long STUDY_ID = 42L;
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Mock authenticated user
        when(oAuth2AuthenticationService.getCurrentUser()).thenReturn(
                new AuthenticatedUser(USER_ID, "Test User", "test@example.com", "Test Inc.",
                        EnumSet.allOf(PlatformRole.class))
        );

        // Mock study validation (used by every endpoint)
        when(studyService.getStudy(anyLong(), any(AuthenticatedUser.class)))
                .thenReturn(Optional.of(new Study().setStudyId(STUDY_ID)));
    }

    /* ==================== Goal Config ==================== */

    @Test
    @DisplayName("GET goal config - should return full config with topics and schedule")
    void testGetGoalConfig() throws Exception {
        StudyGoalConfig config = new StudyGoalConfig()
                .setStudyId(STUDY_ID)
                .setCommitment("strong")
                .setAchievability("medium")
                .setUnderstandability("high");

        List<GoalTopic> topics = List.of(
                new GoalTopic().setStudyId(STUDY_ID).setKey("physical").setTitle("Physical Activity")
        );

        List<GoalAdherenceCheck> checks = List.of(
                new GoalAdherenceCheck().setStudyId(STUDY_ID)
                        .setTitle("morning")
                        .setTime(LocalTime.of(8, 0))
        );

        when(goalService.getGoalConfig(STUDY_ID)).thenReturn(config);
        when(goalService.getGoalTopics(STUDY_ID)).thenReturn(topics);
        when(goalService.getGoalAdherenceChecks(STUDY_ID)).thenReturn(checks);

        mvc.perform(get("/api/v1/studies/{studyId}/goals/config", STUDY_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents.commitment").value("strong"))
                .andExpect(jsonPath("$.consents.achievability").value("medium"))
                .andExpect(jsonPath("$.consents.understandable").value("high"))
                .andExpect(jsonPath("$.topics[0].key").value("physical"))
                .andExpect(jsonPath("$.schedule[0].key").value("morning"))
                .andExpect(jsonPath("$.schedule[0].time").value("08:00:00"));
    }

    @Test
    @DisplayName("GET goal config - returns default empty")
    void testGetGoalConfigNotFound() throws Exception {
        when(goalService.getGoalConfig(STUDY_ID)).thenReturn(null);

        mvc.perform(get("/api/v1/studies/{studyId}/goals/config", STUDY_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents.commitment").isEmpty())
                .andExpect(jsonPath("$.consents.achievability").isEmpty())
                .andExpect(jsonPath("$.consents.understandable").isEmpty())
                .andExpect(jsonPath("$.topics").isArray())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.schedule").isArray())
                .andExpect(jsonPath("$.schedule").isEmpty());
    }

    @Test
    @DisplayName("PUT goal config - should update and return full data")
    void testSetGoalConfig() throws Exception {
        StudyGoalConfigDTO request = new StudyGoalConfigDTO()
                .consents(new StudyGoalConfigConsentsDTO()
                        .commitment("strong")
                        .achievability("medium")
                        .understandable("high"))
                .schedule(List.of(new StudyGoalConfigScheduleInnerDTO()
                        .key(AdherenceCheckScheduleEnumDTO.MORNING)
                        .time(LocalTime.of(9, 0))));

        StudyGoalConfig updatedConfig = new StudyGoalConfig()
                .setStudyId(STUDY_ID)
                .setCommitment("strong")
                .setAchievability("medium")
                .setUnderstandability("high");

        List<GoalAdherenceCheck> updatedChecks = List.of(
                new GoalAdherenceCheck().setStudyId(STUDY_ID).setTitle("morning").setTime(LocalTime.of(9, 0))
        );

        when(goalService.setGoalConfig(any(StudyGoalConfig.class))).thenReturn(updatedConfig);
        when(goalService.setGoalAdherenceChecks(anyLong(), any())).thenReturn(updatedChecks);
        when(goalService.getGoalTopics(STUDY_ID)).thenReturn(List.of());

        mvc.perform(put("/api/v1/studies/{studyId}/goals/config", STUDY_ID)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents.commitment").value("strong"))
                .andExpect(jsonPath("$.schedule[0].key").value("morning"))
                .andExpect(jsonPath("$.schedule[0].time").value("09:00:00"));
    }

    /* ==================== Goal Topics ==================== */

    @Test
    @DisplayName("POST goal topic - creates topic with auto-generated slug")
    void testCreateGoalTopic() throws Exception {
        GoalTopicDTO request = new GoalTopicDTO()
                .title("Physical Activity")
                .description("Daily movement goals");

        GoalTopic created = new GoalTopic()
                .setStudyId(STUDY_ID)
                .setKey("physical-activity")
                .setTitle("Physical Activity")
                .setDescription("Daily movement goals");

        when(goalService.getGoalTopic(STUDY_ID, "physical-activity")).thenReturn(null);
        when(goalService.setGoalTopic(any(GoalTopic.class))).thenReturn(created);

        mvc.perform(post("/api/v1/studies/{studyId}/goals/config/categories/topic", STUDY_ID)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("physical-activity"))
                .andExpect(jsonPath("$.title").value("Physical Activity"));
    }

    @Test
    @DisplayName("POST goal topic - conflict when key exists")
    void testCreateGoalTopicConflict() throws Exception {
        GoalTopicDTO request = new GoalTopicDTO().title("Physical Activity").key("physical");

        when(goalService.getGoalTopic(STUDY_ID, "physical")).thenReturn(new GoalTopic());

        mvc.perform(post("/api/v1/studies/{studyId}/goals/config/categories/topic", STUDY_ID)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT goal topic - updates existing topic")
    void testUpdateGoalTopic() throws Exception {
        GoalTopicDTO request = new GoalTopicDTO()
                .key("physical")
                .title("Updated Title")
                .description("New desc");

        GoalTopic updated = new GoalTopic()
                .setStudyId(STUDY_ID)
                .setKey("physical")
                .setTitle("Updated Title")
                .setDescription("New desc");

        when(goalService.setGoalTopic(any(GoalTopic.class))).thenReturn(updated);

        mvc.perform(put("/api/v1/studies/{studyId}/goals/config/categories/topic/{key}", STUDY_ID, "physical")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE goal topic - returns 204")
    void testDeleteGoalTopic() throws Exception {
        mvc.perform(delete("/api/v1/studies/{studyId}/goals/config/categories/topic/{key}", STUDY_ID, "physical"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    /* ==================== Goal Templates ==================== */

    @Test
    @DisplayName("GET goal templates - returns list")
    void testListGoalTemplates() throws Exception {
        GoalTemplate template = new GoalTemplate()
                .setStudyId(STUDY_ID)
                .setTemplateId(1)
                .setTitle("Daily Steps")
                .setType("steps")
                .setKind("behavioral");

        when(goalService.listGoalTemplates(STUDY_ID)).thenReturn(List.of(template));

        mvc.perform(get("/api/v1/studies/{studyId}/goals/templates", STUDY_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateId").value(1))
                .andExpect(jsonPath("$[0].title").value("Daily Steps"));
    }

    @Test
    @DisplayName("POST goal template - creates and returns template")
    void testAddGoalTemplate() throws Exception {
        GoalTemplateDTO request = new GoalTemplateDTO()
                .studyId(STUDY_ID)
                .title("Daily Steps")
                .participantTitle("Walk 10k steps")
                .participantInfo("Info")
                .type("steps")
                .categories(new GoalTemplateCategoriesDTO()
                        .kind(GoalTemplateCategoriesDTO.KindEnum.BEHAVIORAL)
                        .topics(List.of("physical")));

        GoalTemplate created = new GoalTemplate()
                .setStudyId(STUDY_ID)
                .setTemplateId(10)
                .setTitle("Daily Steps")
                .setType("steps");

        when(goalService.addGoalTemplate(any(GoalTemplate.class))).thenReturn(created);

        mvc.perform(post("/api/v1/studies/{studyId}/goals/templates", STUDY_ID)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(10))
                .andExpect(jsonPath("$.title").value("Daily Steps"));
    }

    @Test
    @DisplayName("PUT goal template - updates template")
    void testUpdateGoalTemplate() throws Exception {
        GoalTemplateDTO request = new GoalTemplateDTO()
                .templateId(5)
                .title("Updated Template");

        GoalTemplate updated = new GoalTemplate()
                .setStudyId(STUDY_ID)
                .setTemplateId(5)
                .setTitle("Updated Template");

        when(goalService.updateGoalTemplate(any(GoalTemplate.class))).thenReturn(updated);

        mvc.perform(put("/api/v1/studies/{studyId}/goals/templates/{templateId}", STUDY_ID, 5)
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Template"));
    }

    @Test
    @DisplayName("DELETE goal template - returns 204")
    void testDeleteGoalTemplate() throws Exception {
        mvc.perform(delete("/api/v1/studies/{studyId}/goals/templates/{templateId}", STUDY_ID, 5))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    /* ==================== Error Cases ==================== */

    @Test
    @DisplayName("Invalid study returns 404 via validation")
    void testStudyNotFound() throws Exception {
        when(studyService.getStudy(anyLong(), any(AuthenticatedUser.class))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/studies/{studyId}/goals/config", STUDY_ID))
                .andExpect(status().isNotFound());
    }
}