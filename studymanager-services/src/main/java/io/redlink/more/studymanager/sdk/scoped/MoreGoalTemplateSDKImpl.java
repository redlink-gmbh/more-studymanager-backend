/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.sdk.scoped;

import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;
import io.redlink.more.studymanager.sdk.MoreSDK;

import java.io.Serializable;
import java.util.Optional;

public class MoreGoalTemplateSDKImpl extends MorePlatformSDKImpl implements MoreGoalTemplateSDK {

    private final int templateId;

    public MoreGoalTemplateSDKImpl(MoreSDK sdk, long studyId, Integer studyGroupId, int templateId) {
        super(sdk, studyId, studyGroupId);
        this.templateId = templateId;
    }

    @Override
    public <T extends Serializable> void setValue(String name, T value) {
        sdk.nvpairs.setGoalTemplateValue(studyId, templateId, name, value);
    }

    @Override
    public <T extends Serializable> Optional<T> getValue(String name, Class<T> tClass) {
        return sdk.nvpairs.getGoalTemplateValue(studyId, templateId, name, tClass);
    }

    @Override
    public void removeValue(String name) {
        sdk.nvpairs.removeGoalTemplateValue(studyId, templateId, name);
    }

    @Override
    public int getTemplateId() {
        return getTemplateId();
    }
}
