/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Oesterreichische Vereinigung zur
 * Foerderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.auth.model;

import java.io.Serializable;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Provide information about the study context of the authenticated user
 *
 * @param studyId             the id of the study the authenticated user is participating in
 * @param participantId       the id of the study participant for the authenticated user
 * @param rawStudyGroupId     the optional study group id (negative number if not applicable)
 * @param observationGroupIds the observation group ids for the study participant
 * @param studyActive         if the study is currently active
 * @param participantActive   if the participant is currently active
 */
public record RoutingInfo(
        long studyId,
        int participantId,
        int rawStudyGroupId,
        Set<Integer> observationGroupIds,
        boolean studyActive,
        boolean participantActive
) implements Serializable {

    public RoutingInfo(long studyId,
                       int participantId,
                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt studyGroupId,
                       Set<Integer> observationGroupIds,
                       boolean studyActive,
                       boolean participantActive
    ) {
        this(studyId, participantId, studyGroupId.orElse(Integer.MIN_VALUE), observationGroupIds, studyActive, participantActive);
    }

    public OptionalInt studyGroupId() {
        if (this.rawStudyGroupId < 0) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(rawStudyGroupId);
        }
    }

    public boolean acceptData() {
        return studyActive && participantActive;
    }

    public String participantRef() {
        return studyId + ":" + participantId;
    }
}
