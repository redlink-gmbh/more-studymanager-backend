/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Oesterreichische Vereinigung zur
 * Foerderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.model.gateway;

import java.io.Serializable;
import java.util.OptionalInt;
import java.util.Set;

public record RoutingInfo(
        Long studyId,
        Integer participantId,
        Integer studyGroupId,
        Set<Integer> observationGroupIds,
        boolean studyActive,
        boolean participantActive
) implements Serializable {

    public boolean acceptData() {
        return studyActive && participantActive;
    }
}
