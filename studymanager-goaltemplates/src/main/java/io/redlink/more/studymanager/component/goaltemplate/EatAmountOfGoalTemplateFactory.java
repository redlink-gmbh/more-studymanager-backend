package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringTemplateValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;
import java.util.Set;

public class EatAmountOfGoalTemplateFactory extends AbstractAmountOfGoalTemplateFactory<AmoutOfGoalTemplate<GoalTemplateProperties>,GoalTemplateProperties>  {


    public EatAmountOfGoalTemplateFactory() {
        super("eat-amount-of");
    }

    public List<Value> getProperties() {
        return List.of(
                CONFIG_SECTION_CONFIGURATION,
                APP_TITLE,
                APP_DESCRIPTION,
                GOAL_TITLE_STATE,
                SHOW_AS_TODO_ITEM_STATE,
                CUSTOM_SHOW_AS_TODO_ITEM_STATE,

                CONFIG_SECTION_GOAL_CONFIGURATION,
                new StringTemplateValue(
                        "goal-preview",
                        Set.of(
                            CONFIGS_GOAL_AMOUNT_VALUE.getId(),
                            CONFIGS_GOAL_AMOUNT_UNIT.getId(),
                            DAYS_OF_WEEK.getId()
                        ))
                    .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.name")
                    .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.description")
                    .setDefaultValue("Ich esse mindestens <goal.amount> Portionen <goal.unit> [an <days-of-week> Tagen der Woche]")
                    .setImmutable(false),
                CONFIGS_GOAL_AMOUNT_GROUP,
                CONFIGS_GOAL_AMOUNT_VALUE,
                CONFIGS_GOAL_AMOUNT_UNIT,
                DAYS_OF_WEEK,

                CONFIG_SECTION_STATUS,
                STATUS_100_PERCENT_REACHED,
                STATUS_75_PERCENT_REACHED.copyOf()
                        .setDefaultValue("Dein Ziel ist zum greifen nah. Ein bisschen mehr und Du hast es geschafft!"),
                STATUS_0_PERCENT_REACHED.copyOf()
                        .setDefaultValue("Du bist auf den richtigen Weg. Jede Portion zählt für Dein wohlbefinden."),

                CONFIG_SECTION_SELF_REPORT,
                SELF_REPORT_QUESTION.copyOf()
                        .setDefaultValue("Wie viele Portionen hast Du heute gegessen? Bitte trage den Wert ein.")
        );
    }

    @Override
    public Class<GoalTemplateProperties> getPropertyClass() {
        return GoalTemplateProperties.class;
    }

    @Override
    public AmoutOfGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new AmoutOfGoalTemplate<>(sdk, properties);
    }
}
