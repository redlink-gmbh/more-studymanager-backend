/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.controller.studymanager;

import com.fasterxml.jackson.databind.JsonNode;
import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.factory.TriggerFactory;
import io.redlink.more.studymanager.core.model.User;
import io.redlink.more.studymanager.model.AuthenticatedUser;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ComponentApiV1Controller.class})
@AutoConfigureMockMvc(addFilters = false)
class ComponentControllerTest {

    @MockitoBean
    private OAuth2AuthenticationService authenticationService;

    @Autowired
    private MockMvc mvc;

    //Test configuration that registers a observation-, trigger-, action- and goalTemplateFactory for testing
    @TestConfiguration
    static class TestComponentConfig {

        ObservationFactory observationFactory;
        TriggerFactory triggerFactory;
        ActionFactory actionFactory;
        GoalTemplateFactory goalTemplateFactory;

        public TestComponentConfig(){
            this.observationFactory = mock(ObservationFactory.class);
            when(observationFactory.getId()).thenReturn("my-test-observation");

            this.triggerFactory = mock(TriggerFactory.class);
            when(triggerFactory.getId()).thenReturn("my-test-trigger");

            this.actionFactory = mock(ActionFactory.class);
            when(actionFactory.getId()).thenReturn("my-test-action");

            this.goalTemplateFactory = mock(GoalTemplateFactory.class);
            when(goalTemplateFactory.getId()).thenReturn("my-test-goal-template");
        }

        @Bean("my-test-observation")
        public ObservationFactory getObservationFactory() {
            return observationFactory;
        }

        @Bean("my-test-trigger")
        public TriggerFactory getTriggerFactory() {
            return triggerFactory;
        }

        @Bean("my-test-action")
        public ActionFactory getActionFactory() {
            return actionFactory;
        }

        @Bean("my-test-goal-template")
        public GoalTemplateFactory getGoalTemplateFactory() {
            return goalTemplateFactory;
        }
    }

    @Autowired
    private TestComponentConfig testComponentConfig;

    @Captor
    ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor;

    @Test
    void testComponentSpecificEndpointExists() throws Exception {

        AuthenticatedUser user = new AuthenticatedUser("user1", "", "","", Set.of());
        when(authenticationService.getCurrentUser()).thenReturn(user);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/observation/my-test-observation/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"hello\":\"world\"}"))
                .andExpect(status().isOk());

        verify(testComponentConfig.observationFactory).handleAPICall(anyString(), any(User.class), jsonNodeArgumentCaptor.capture());
        String value = jsonNodeArgumentCaptor.getValue().get("hello").asText();
        Assertions.assertEquals("world", value);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/action/my-test-action/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"hello\":\"world\"}"))
                .andExpect(status().isOk());

        verify(testComponentConfig.observationFactory).handleAPICall(anyString(), any(User.class), jsonNodeArgumentCaptor.capture());
        value = jsonNodeArgumentCaptor.getValue().get("hello").asText();
        Assertions.assertEquals("world", value);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/trigger/my-test-trigger/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"hello\":\"world\"}"))
                .andExpect(status().isOk());

        verify(testComponentConfig.observationFactory).handleAPICall(anyString(), any(User.class), jsonNodeArgumentCaptor.capture());
        value = jsonNodeArgumentCaptor.getValue().get("hello").asText();
        Assertions.assertEquals("world", value);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/goalTemplate/my-test-goal-template/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"hello\":\"world\"}"))
                .andExpect(status().isOk());

        verify(testComponentConfig.observationFactory).handleAPICall(anyString(), any(User.class), jsonNodeArgumentCaptor.capture());
        value = jsonNodeArgumentCaptor.getValue().get("hello").asText();
        Assertions.assertEquals("world", value);

    }

    @Test
    void testComponentSpecificEndpointDoesNotExist() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/observation/another-test-observation/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/trigger/another-test-trigger/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/action/another-test-action/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/components/goalTemplate/another-test-goal-template/api/my-test-slug")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().isNotFound());

    }


}
