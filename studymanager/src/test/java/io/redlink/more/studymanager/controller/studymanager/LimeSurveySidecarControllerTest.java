package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.configuration.WebSecurityConfiguration;
import io.redlink.more.studymanager.repository.UserRepository;
import io.redlink.more.studymanager.sdk.MoreSDK;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import io.redlink.more.studymanager.service.ObservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LimeSurveySidecarController.class,
        properties = "more.components.lime-survey-observation.baseUrl=https://lime.example.org/")
@Import(WebSecurityConfiguration.class)
class LimeSurveySidecarControllerTest {
    @MockitoBean
    ObservationService observationService;
    @MockitoBean
    MoreSDK sdk;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @MockitoBean
    OAuth2AuthenticationService oAuth2AuthenticationService;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Test
    void redirectsToLimeSurveyAdminWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/components/observation/lime-survey-observation/admin"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://lime.example.org/admin/"));
    }
}
