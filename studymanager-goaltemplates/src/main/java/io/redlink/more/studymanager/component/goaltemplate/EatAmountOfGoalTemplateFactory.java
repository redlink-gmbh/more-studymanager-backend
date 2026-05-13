package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ComponentFactory;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import static io.redlink.more.studymanager.component.goaltemplate.AbstractAmountOfGoalTemplateFactory.*;

import java.util.List;

public class EatAmountOfGoalTemplateFactory extends AbstractAmountOfGoalTemplateFactory<AmoutOfGoalTemplate<GoalTemplateProperties>,GoalTemplateProperties>  {


    public EatAmountOfGoalTemplateFactory() {
        super("eat-amount-of");
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
                        .setDefaultValue("Ich esse mindestens <goal.amount> Portionen <goal.unit> [an <days-of-week> Tagen der Woche]")
                        .setImmutable(true),
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_GROUP,
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_VALUE,
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_UNIT,
                GoalTemplateFactory.DAYS_OF_WEEK,

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_STATUS,
                new StringValue("status-100-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-100-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-100-reached.description")
                        .setDefaultValue("Du hast dein Tagesziel erfolgreich gemeistert. Weiter so!"),
                new StringValue("status-75-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-75-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-75-reached.description")
                        .setDefaultValue("Dein Ziel ist zum greifen nah. Ein bisschen mehr <goal.unit> und Du hast es geschafft!"),
                new StringValue("status-not-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-not-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-not-reached.description")
                        .setDefaultValue("Du bist auf den richtigen Weg. Jede Portion <goal.unit> zählt für Dein wohlbefinden."),

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_SELF_REPORT,
                new StringValue("self-report-question")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.description")
                        .setDefaultValue("Wie viele Portionen <goal.unit> hast Du heute gegessen? Bitte trage den Wert ein."),
                GoalTemplateFactory.SELF_REPORT_TIME_EVENING
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
