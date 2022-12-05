/*
 * Copyright (c) 2022 Redlink GmbH.
 */
package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.CurrentUserDTO;
import io.redlink.more.studymanager.api.v1.model.UserSearchResultListDTO;
import io.redlink.more.studymanager.api.v1.webservices.UsersApi;
import io.redlink.more.studymanager.model.transformer.UserInfoTransformer;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserApiV1Controller implements UsersApi {

    private final OAuth2AuthenticationService authService;

    public UserApiV1Controller(OAuth2AuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<CurrentUserDTO> getCurrentUser() {
        return ResponseEntity.ok(
                UserInfoTransformer.toCurrentUserDTO(
                        authService.getCurrentUser()
                )
        );
    }

    @Override
    public ResponseEntity<UserSearchResultListDTO> findUsers(String q, Integer offset, Integer limit) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
