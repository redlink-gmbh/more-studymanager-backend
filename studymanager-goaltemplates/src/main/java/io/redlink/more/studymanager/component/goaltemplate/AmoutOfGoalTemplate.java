package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.component.Observation;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

public class AmoutOfGoalTemplate<C extends GoalTemplateProperties> extends GoalTemplate<C> {


    protected AmoutOfGoalTemplate(MoreGoalTemplateSDK sdk, C properties) throws ConfigurationValidationException {
        super(sdk, properties);
    }


    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
