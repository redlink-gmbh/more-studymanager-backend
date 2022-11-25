package io.redlink.more.studymanager.configuration;

import io.redlink.more.studymanager.properties.MoreAuthProperties;
import io.redlink.more.studymanager.repository.UserRepository;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(MoreAuthProperties.class)
public class WebSecurityConfiguration {

    final ClientRegistrationRepository clientRegistrationRepository;

    final MoreAuthProperties moreAuthProperties;

    public WebSecurityConfiguration(ClientRegistrationRepository clientRegistrationRepository, MoreAuthProperties moreAuthProperties) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.moreAuthProperties = moreAuthProperties;
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http,
                                              OAuth2AuthenticationService oAuth2AuthenticationService,
                                              OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
                                              UserRepository userRepository) throws Exception {
        // Basics
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        http.cors().disable();

        // Restricted Paths
        http.authorizeRequests()
                .antMatchers("/api", "/api/v1/me").permitAll()
                .antMatchers("/api/v1/**").authenticated()
                .antMatchers("/login/init").authenticated()
                .antMatchers("/actuator/**").hasIpAddress("127.0.0.1/8")
                .anyRequest().denyAll();

        // API-Calls should not be redirected to the login page, but answered with a 401
        http.exceptionHandling()
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/**"));

        // Logout Config
        http.logout()
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .logoutSuccessUrl("/");

        // Enable OAuth2
        http.oauth2Login()
                // register oauth2-provider under this baseurl to simplify routing
                .authorizationEndpoint().baseUri("/login/oauth").and()
                .authorizedClientService(
                        new UserSyncingOAuth2AuthorizedClientService(oAuth2AuthorizedClientService, oAuth2AuthenticationService, userRepository)
                );

        // Enable OAuth2 client_credentials flow (insomnia)
        http.oauth2ResourceServer().jwt();

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
        return oidcLogoutSuccessHandler;
    }

    @Bean
    protected OAuth2AuthenticationService oAuth2AuthenticationService() {
        return new OAuth2AuthenticationService(moreAuthProperties);
    }

    static class UserSyncingOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

        private final OAuth2AuthorizedClientService delegate;
        private final OAuth2AuthenticationService oAuth2AuthenticationService;
        private final UserRepository userRepository;

        UserSyncingOAuth2AuthorizedClientService(OAuth2AuthorizedClientService delegate,
                                                 OAuth2AuthenticationService oAuth2AuthenticationService,
                                                 UserRepository userRepository) {
            this.delegate = delegate;
            this.oAuth2AuthenticationService = oAuth2AuthenticationService;
            this.userRepository = userRepository;
        }

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
            return delegate.loadAuthorizedClient(clientRegistrationId, principalName);
        }

        @Override
        public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication authentication) {
            var user = oAuth2AuthenticationService.getAuthenticatedUser(authentication);
            if (user.id() != null) {
                userRepository.save(user);
            }
            delegate.saveAuthorizedClient(authorizedClient, authentication);
        }

        @Override
        public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
            delegate.removeAuthorizedClient(clientRegistrationId, principalName);
        }
    }

}
