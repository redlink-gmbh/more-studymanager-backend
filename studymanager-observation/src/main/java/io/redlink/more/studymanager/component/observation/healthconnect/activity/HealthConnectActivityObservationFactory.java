package io.redlink.more.studymanager.component.observation.healthconnect.activity;

import io.redlink.more.studymanager.component.observation.measurement.GenericMeasurementSets;
import io.redlink.more.studymanager.core.component.Observation;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.io.Visibility;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;

public class HealthConnectActivityObservationFactory<C extends Observation<P>, P extends ObservationProperties> extends ObservationFactory<C, P> {

    private static final Visibility visibility = new Visibility(true, false);

    @Override
    public HealthConnectActivityObservation create(MoreObservationSDK sdk, ObservationProperties properties) throws ConfigurationValidationException {
        return new HealthConnectActivityObservation(sdk, validate((P) properties));
    }

    @Override
    public MeasurementSet getMeasurementSet() {
        return GenericMeasurementSets.ACTIVITY;
    }

    @Override
    public String getId() {
        return "health-connect-activity-observation";
    }

    @Override
    public String getTitle() {
        return "observation.factory.health-connect.activity.title";
    }

    @Override
    public String getDescription() {
        return "observation.factory.health-connect.activity.description";
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }
}
