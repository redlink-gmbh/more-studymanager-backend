/*
 * Copyright (c) 2022 Redlink GmbH.
 */
package io.redlink.more.studymanager.model.transformer;

import io.redlink.more.studymanager.api.v1.model.CollaboratorDTO;
import io.redlink.more.studymanager.api.v1.model.CollaboratorDetailsDTO;
import io.redlink.more.studymanager.api.v1.model.CollaboratorRoleDetailsDTO;
import io.redlink.more.studymanager.api.v1.model.CurrentUserDTO;
import io.redlink.more.studymanager.api.v1.model.UserInfoDTO;
import io.redlink.more.studymanager.api.v1.model.UserSearchResultListResultDTO;
import io.redlink.more.studymanager.model.AuthenticatedUser;
import io.redlink.more.studymanager.model.MoreUser;
import io.redlink.more.studymanager.model.SearchResult;
import io.redlink.more.studymanager.model.StudyRole;
import io.redlink.more.studymanager.model.StudyUserRoles;
import io.redlink.more.studymanager.model.User;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserInfoTransformer {

    private UserInfoTransformer() {}

    public static UserInfoDTO toUserInfoDTO(User user) {
        if (user == null) return null;

        return new UserInfoDTO()
                .uid(user.id())
                .name(user.fullName())
                .email(user.email())
                .institution(user.institution());
    }

    public static CurrentUserDTO toCurrentUserDTO(AuthenticatedUser user) {
        if (user == null) return null;

        return new CurrentUserDTO()
                .uid(user.id())
                .name(user.fullName())
                .email(user.email())
                .institution(user.institution())
                .completeProfile(user.isValid())
                .roles(RoleTransformer.toPlatformRolesDTO(user.roles()));
    }

    public static CollaboratorDTO toCollaboratorDTO(MoreUser user, Set<StudyRole> roles) {
        return new CollaboratorDTO()
                .user(toUserInfoDTO(user))
                .roles(RoleTransformer.toStudyRolesDTO(roles))
                ;
    }

    public static CollaboratorDetailsDTO toCollaboratorDetailsDTO(StudyUserRoles usr) {
        return new CollaboratorDetailsDTO()
                .user(toUserInfoDTO(usr.user()))
                .roles(toCollaboratorRoleDetailsDTO(usr.roles()));
    }

    private static Set<CollaboratorRoleDetailsDTO> toCollaboratorRoleDetailsDTO(Set<StudyUserRoles.StudyRoleDetails> roles) {
        return roles.stream()
                .map(UserInfoTransformer::toCollaboratorRoleDetailsDTO)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static CollaboratorRoleDetailsDTO toCollaboratorRoleDetailsDTO(StudyUserRoles.StudyRoleDetails role) {
        return new CollaboratorRoleDetailsDTO()
                .role(RoleTransformer.toStudyRoleDTO(role.role()))
                .assignedBy(toUserInfoDTO(role.creator()))
                .assignedAt(role.created().atOffset(ZoneOffset.UTC))
                ;
    }

    public static UserSearchResultListResultDTO toUserSearchResultListDTO(SearchResult<? extends User> searchResult) {
        return new UserSearchResultListResultDTO()
                .numFound(searchResult.numFound())
                .start(searchResult.offset())
                .users(
                        searchResult.content().stream()
                                .map(UserInfoTransformer::toUserInfoDTO)
                                .toList()
                )
                ;
    }
}
