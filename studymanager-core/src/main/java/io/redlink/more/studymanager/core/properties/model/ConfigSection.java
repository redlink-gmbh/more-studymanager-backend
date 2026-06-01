/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

/**
 * A ConfigSection is a marker property that allows to goup different parts of the
 * configuration.
 *
 * It defines a title and an optional description and visually group all Values until the
 * next ConfigSection in the configuration dialog. It does not contribute to the
 * configuration.
 *
 * The ConfigSection property is always:
 * <ul>
 *     <li>{@link Value#isImmutable()} == true</li>
 *     <li>{@link Value#getDefaultValue()} == null</li>
 *     <li>{@link Value#isRequired()} == false</li>
 * </ul>
 */
public class ConfigSection extends Value<Void> {
    public ConfigSection(String id) {
        super(id);
    }

    @Override
    public Class<Void> getValueType() {
        return Void.class;
    }

    @Override
    public String getType() {
        return "GROUPING";
    }

    @Override
    public final boolean isImmutable() {
        return true;
    }

    @Override
    public final Void getDefaultValue() {
        return null;
    }

    @Override
    public final boolean isRequired() {
        return false;
    }
}
