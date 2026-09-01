/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.sdk.scoped;

import io.redlink.more.studymanager.core.sdk.MoreGoalSDK;
import io.redlink.more.studymanager.sdk.MoreSDK;

import java.io.Serializable;
import java.util.Optional;

public class MoreGoalSDKImpl extends MorePlatformSDKImpl implements MoreGoalSDK {

    private final int goalId;

    public MoreGoalSDKImpl(MoreSDK sdk, long studyId, Integer studyGroupId, int goalId) {
        super(sdk, studyId, studyGroupId);
        this.goalId = goalId;
    }

    @Override
    public <T extends Serializable> void setValue(String name, T value) {
        sdk.nvpairs.setGoalValue(studyId, goalId, name, value);
    }

    @Override
    public <T extends Serializable> Optional<T> getValue(String name, Class<T> tClass) {
        return sdk.nvpairs.getGoalValue(studyId, goalId, name, tClass);
    }

    @Override
    public void removeValue(String name) {
        sdk.nvpairs.removeGoalValue(studyId, goalId, name);
    }

    @Override
    public int getGoalId() {
        return goalId;
    }

}
