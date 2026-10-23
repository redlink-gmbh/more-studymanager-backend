/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.component;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.ComponentProperties;
import io.redlink.more.studymanager.core.properties.ObservationProperties;

public abstract class Component<P extends ComponentProperties> {
    protected final P properties;

    Component(P properties) throws ConfigurationValidationException {
        this.properties = properties;
    }

    public abstract void activate();

    public abstract void deactivate();

    /**
     * Called after the component's properties were updated while the study is active.
     *
     * @param previous the properties before the update
     * @param title    the (current) title of the observation
     */
    public void onUpdate(ObservationProperties previous, String title) {
        // no action
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "properties=" + properties +
                '}';
    }
}
