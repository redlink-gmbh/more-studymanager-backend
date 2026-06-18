package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

public class MedicationGoalTemplate<C extends GoalTemplateProperties> extends GoalTemplate<C> {


    protected MedicationGoalTemplate(MoreGoalTemplateSDK sdk, C properties) throws ConfigurationValidationException {
        super(sdk, properties);
    }


    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
