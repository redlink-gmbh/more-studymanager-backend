package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.StringTemplateValue;
import io.redlink.more.studymanager.core.properties.model.StringTextValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;
import java.util.Set;


public class TrinkAmountOfGoalTemplateFactory extends AbstractAmountOfGoalTemplateFactory<AmoutOfGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties>  {

    public TrinkAmountOfGoalTemplateFactory() {
        super("trink-amount-of");
    }

    public List<Value> getProperties() {
        return List.of(
                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_CONFIGURATION,
                GoalTemplateFactory.APP_TITLE,
                GoalTemplateFactory.APP_DESCRIPTION,
                GoalTemplateFactory.GOAL_TITLE_STATE,

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_GOAL_CONFIGURATION,
                new StringTemplateValue(
                        "goal-preview",
                        Set.of(
                                CONFIGS_GOAL_AMOUNT_VALUE.getId(),
                                CONFIGS_GOAL_AMOUNT_UNIT.getId(),
                                DAYS_OF_WEEK.getId()
                        ))
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal-preview.description")
                        .setDefaultValue("Ich trinke mindestens <goal.amount> <goal.unit> [an <days-of-week> Tagen der Woche]")
                        .setImmutable(false),
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_GROUP,
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_VALUE,
                AbstractAmountOfGoalTemplateFactory.CONFIGS_GOAL_AMOUNT_UNIT,
                GoalTemplateFactory.DAYS_OF_WEEK,

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_STATUS,
                new StringTextValue("status-100-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-100-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-100-reached.description")
                        .setDefaultValue("Du hast dein Tagesziel erfolgreich gemeistert. Weiter so!"),
                new StringTextValue("status-75-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-75-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-75-reached.description")
                        .setDefaultValue("Dein Ziel ist zum greifen nah. Ein bisschen und Du hast es geschafft!"),
                new StringTextValue("status-not-reached")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "status-not-reached.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "status-not-reached.description")
                        .setDefaultValue("Du bist auf dem richtigen Weg. Jede Schluck zählt für Dein wohlbefinden."),

                AbstractAmountOfGoalTemplateFactory.CONFIG_SECTION_SELF_REPORT,
                new StringTextValue("self-report-question")
                        .setName(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.name")
                        .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "self-report-question.description")
                        .setDefaultValue("Wie viele Einheiten hast Du heute getrunken? Bitte trage den Wert ein.")
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
