package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringTextValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;
import java.util.Set;


public class BooleanGoalTemplateFactory extends AbstractBooleanGoalTemplateFactory<BooleanGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties>  {

    public BooleanGoalTemplateFactory() {
        super("boolean");
    }

    public List<Value> getProperties() {
        return List.of(
                CONFIG_SECTION_CONFIGURATION,
                APP_TITLE,
                APP_DESCRIPTION,
                //GOAL_TITLE_STATE, -> title can not be modified by user!
                //ALLOW_INSTANCES_STATE -> not multiple instances of boolean goals
                CUSTOM_ADHERENCE_CHECKS_STATE, //allow to enable/disable custom adherence checks for multiple checks per day

                CONFIG_SECTION_GOAL_CONFIGURATION,
                CONFIGS_GOAL_QUESTION,
                CONFIGS_GOAL_DESIRED,
                DAYS_OF_WEEK,

                ADHERENCE_CHECK_BASED_SCHEDULE_STATE,
                SHOW_AS_TODO_ITEM_STATE,

                CONFIG_SECTION_SELF_REPORT,
                new StringTextValue("self-report-question")
                        .setName(BOOLEAN_PROPERTY_PREFIX + "self-report-question.name")
                        .setDescription(BOOLEAN_PROPERTY_PREFIX + "self-report-question.description")
                        .setRequired(false)
                        .setDefaultValue("Hast du Heute dein Ziel erreicht?")
        );
    }

    @Override
    public Class<GoalTemplateProperties> getPropertyClass() {
        return GoalTemplateProperties.class;
    }

    @Override
    public BooleanGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new BooleanGoalTemplate<>(sdk, properties);
    }

}
