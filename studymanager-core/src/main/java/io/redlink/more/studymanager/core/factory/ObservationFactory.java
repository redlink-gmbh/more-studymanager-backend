/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.factory;

import io.redlink.more.studymanager.core.component.Observation;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.io.Visibility;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;

import java.util.Map;

public abstract class ObservationFactory<C extends Observation<P>, P extends ObservationProperties> extends ComponentFactory<C, P> {
    public abstract C create(MoreObservationSDK sdk, P properties) throws ConfigurationValidationException;

    @Override
    public Class<ObservationProperties> getPropertyClass() {
        return ObservationProperties.class;
    }

    public abstract MeasurementSet getMeasurementSet();

    @Deprecated
    public Boolean getHidden() {
        return true;
    }

    public Visibility getVisibility() {
        return Visibility.DEFAULT;
    }

    public boolean isResyncable() {
        return false;
    }

    /** Value for {@link #getValidationFilter()}: the field must be given and not null, whatever its value. */
    public static final Object VALUE_PRESENT = new Object() {
        @Override
        public String toString() {
            return "<present>";
        }
    };

    /**
     * Required field values (measurement id → value, or {@link #VALUE_PRESENT}) a datapoint must have to be included
     * in the data summary passed to {@link Observation#validateData}. Empty by default, i.e. all datapoints count.
     */
    public Map<String, Object> getValidationFilter() {
        return Map.of();
    }

    /**
     * If {@code true}, {@link Observation#validateData} gets a summary of all data from the start of the study up to
     * the end of the occurrence, instead of only the data within the occurrence.
     */
    public boolean isValidationCumulative() {
        return false;
    }

    /**
     * Called before the properties of an observation are stored. Allows to reject changes that depend on the
     * previous state, or to set server-side values.
     *
     * @param previous the stored properties, {@code null} if the observation is created
     * @param updated  the properties to store
     * @return the properties to store
     * @throws ConfigurationValidationException if the update is not allowed
     */
    public P prepareUpdate(P previous, P updated) throws ConfigurationValidationException {
        return updated;
    }

    /**
     * Like {@link #prepareUpdate(ObservationProperties, ObservationProperties)}, but also tells whether the study
     * state allows removing runtime content (draft or paused study).
     */
    public P prepareUpdate(P previous, P updated, boolean deletionAllowed) throws ConfigurationValidationException {
        return prepareUpdate(previous, updated);
    }

    /**
     * Study-wide observations have no configurable schedule: they are simply on or off
     * and run for the whole study (effective study start .. effective study end).
     */
    public boolean isStudyWide() {
        return false;
    }
}
