package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringTemplateValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;
import java.util.Set;

public class ReduceAmountOfGoalTemplateFactory extends AbstractAmountOfGoalTemplateFactory<AmoutOfGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties>  {

    public ReduceAmountOfGoalTemplateFactory(){
        super("reduce-amount-of");
    }

    public List<Value> getProperties() {
        return List.of(
                CONFIG_SECTION_CONFIGURATION,
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
                        .setDefaultValue("Ich <goal.activity> maximal <goal.amount> <goal.unit> [an maximal <days-of-week> Tagen in der Woche]")
                        .setImmutable(false),
                CONFIGS_GOAL_AMOUNT_GROUP,
                new StringValue("goal.activity")
                        .setName(GOAL_TEMPLATE_PROPERTY_PREFIX + "reduceAmountOf.goal.activity.name")
                        .setDescription(GOAL_TEMPLATE_PROPERTY_PREFIX + "reduceAmountOf.goal.activity.description")
                        .setDefaultValue("konsumiere")
                        .setRequired(true),
                CONFIGS_GOAL_AMOUNT_VALUE,
                CONFIGS_GOAL_AMOUNT_UNIT,
                DAYS_OF_WEEK,

                CONFIG_SECTION_STATUS,
                STATUS_NOT_CONSUMED.copyOf()
                        .setDefaultValue("Starker Tag! Du hast heute nichts konsumiert."),
                STATUS_UNDER_LIMIT.copyOf()
                        .setDefaultValue("Du gemacht. Du bist im Ziel geblieben und hast die Kontrolle behalten. Weiter so!"),
                STATUS_OVER_LIMIT.copyOf()
                        .setDefaultValue("Du hast heute mehr konsumiert als ausgemacht. Bleib dran. Morgen geht es wieder besser."),

                CONFIG_SECTION_SELF_REPORT,
                SELF_REPORT_QUESTION.copyOf()
                        .setDefaultValue("Wie viele Einheiten hast Du heute <goal.activity>? Bitte trage den Wert ein.")
        );
    }


    @Override
    public AmoutOfGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new AmoutOfGoalTemplate<>(sdk, properties);
    }


}
