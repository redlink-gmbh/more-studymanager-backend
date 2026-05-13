/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.component;

import io.redlink.more.studymanager.core.datavalidity.ObservationDataState;
import io.redlink.more.studymanager.core.datavalidity.ObservationDataSummary;
import io.redlink.more.studymanager.core.datavalidity.ObservationValidationResult;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.io.TimeRange;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreGoalSDK;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;
import io.redlink.more.studymanager.core.ui.DataView;
import io.redlink.more.studymanager.core.ui.DataViewInfo;

import java.time.Instant;

public abstract class GoalTemplate<C extends GoalTemplateProperties> extends Component<C> {

    protected final MoreGoalTemplateSDK sdk;
    protected GoalTemplate(MoreGoalTemplateSDK sdk, C properties) throws ConfigurationValidationException {
        super(properties);
        this.sdk = sdk;
    }

    @Override
    public void activate() {
        // no action
    }

    @Override
    public void deactivate() {
        // no action
    }


}
