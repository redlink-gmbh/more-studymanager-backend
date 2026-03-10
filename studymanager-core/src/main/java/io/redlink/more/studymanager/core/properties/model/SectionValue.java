/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

import io.redlink.more.studymanager.core.properties.ComponentProperties;
import io.redlink.more.studymanager.core.validation.ValidationIssue;

public class SectionValue extends Value<String> {
    public SectionValue(String id, String title) {
        super(id);
        setDefaultValue(title);
    }

    @Override
    protected ValidationIssue doValidate(String s) {
        if (isRequired() && (s == null || s.trim().isEmpty())) {
            return ValidationIssue.requiredMissing(this);
        }
        return super.doValidate(s);
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public String getValue(ComponentProperties properties) {
        return getDefaultValue();
    }

    @Override
    public String getType() {
        return "SECTION";
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }
}
