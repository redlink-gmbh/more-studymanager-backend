package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ComponentFactory;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;

public class ReduceAmountOfGoalTemplateFactory extends AbstractAmountOfGoalTemplateFactory<AmoutOfGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties>  {

    public ReduceAmountOfGoalTemplateFactory(){
        super("reduce-amount-of");
    }

    public List<Value> getProperties() {
        return List.of(
                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_CONFIGURATION,
                GoalTemplateFactory.APP_TITLE,
                GoalTemplateFactory.APP_DESCRIPTION,
                GoalTemplateFactory.GOAL_TITLE_STATE,

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_GOAL_CONFIGURATION,
                new StringValue("goal-preview")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.description")
                        .setDefaultValue("Ich <goal.activity> maximal <goal.amount> <goal.unit> [an maximal <days-of-week> Tagen in der Woche]")
                        .setImmutable(true),
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_GROUP,
                new StringValue("goal.activity")
                        .setName(GOAL_TEMPLATE_PROPERTY_PREFIX + "reduceAmountOf.goal.activity.name")
                        .setDescription(GOAL_TEMPLATE_PROPERTY_PREFIX + "reduceAmountOf.goal.activity.description")
                        .setDefaultValue("konsumiere")
                        .setRequired(true),
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_VALUE,
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_UNIT,
                GoalTemplateFactory.DAYS_OF_WEEK,

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_STATUS,
                new StringValue("status-day-not-consumed")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-day-not-consumed.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-day-not-consumed.description")
                        .setDefaultValue("Starker Tag! Du hast heute nichts konsumiert."),
                new StringValue("status-day-under-limit")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-day-under-limit.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-day-under-limit.description")
                        .setDefaultValue("Du gemacht. Du bist im Ziel geblieben und hast die Kontrolle behalten. Weiter so!"),
                new StringValue("status-day-over-limit")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-day-over-limit.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-day-over-limit.description")
                        .setDefaultValue("Du hast heute mehr konsumiert als ausgemacht. Bleib dran. Morgen geht es wieder besser."),

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_SELF_REPORT,
                new StringValue("self-report-question")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.description")
                        .setDefaultValue("Wie viele <goal.unit> hast Du heute <goal.activity>? Bitte trage den Wert ein."),
                GoalTemplateFactory.SELF_REPORT_TIME_EVENING
        );
    }


    @Override
    public AmoutOfGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new AmoutOfGoalTemplate<>(sdk, properties);
    }


}
