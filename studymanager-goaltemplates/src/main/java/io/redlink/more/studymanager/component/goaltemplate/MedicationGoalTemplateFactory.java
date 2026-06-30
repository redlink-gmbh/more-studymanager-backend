package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;


public class MedicationGoalTemplateFactory extends AbstractBooleanGoalTemplateFactory<MedicationGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties>  {

    public MedicationGoalTemplateFactory() {
        super("medication");
    }

    public List<Value> getProperties() {
        return List.of(
                CONFIG_SECTION_CONFIGURATION,
                //Changing the Goal Title is allowed by participants
                GOAL_TITLE_STATE.copyOf()
                        .setDefaultValue(true)
                        .setImmutable(true),
                //participants can create multiple instances
                ALLOW_INSTANCES_STATE.copyOf()
                        .setDefaultValue(true)
                        .setImmutable(true),
                //participants are expected to assign adherence checks
                CUSTOM_ADHERENCE_CHECKS_STATE.copyOf()
                        .setDefaultValue(true)
                        .setImmutable(true),
                //the schedule is based on adherence checks (not the whole day)
                ADHERENCE_CHECK_BASED_SCHEDULE_STATE.copyOf()
                        .setDefaultValue(true)
                        .setImmutable(true),
                //medication goals are always shown in the to do list of the today tab
                SHOW_AS_TODO_ITEM_STATE.copyOf()
                        .setDefaultValue(true)
                        .setImmutable(true),

                CONFIG_SECTION_SELF_REPORT,
                SELF_REPORT_QUESTION.copyOf()
                        .setDefaultValue("Hast du das Medikament eingenommen?")
        );
    }

    @Override
    public Class<GoalTemplateProperties> getPropertyClass() {
        return GoalTemplateProperties.class;
    }

    @Override
    public MedicationGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new MedicationGoalTemplate<>(sdk, properties);
    }

}
