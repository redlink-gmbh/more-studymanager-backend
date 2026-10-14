package io.redlink.more.studymanager.component.observation.healthconnect.activity;

import io.redlink.more.studymanager.core.component.Observation;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;

public class HealthConnectActivityObservation<C extends ObservationProperties> extends Observation<C> {
    protected HealthConnectActivityObservation(MoreObservationSDK sdk, C properties) throws ConfigurationValidationException {
        super(sdk, properties);
    }
}
