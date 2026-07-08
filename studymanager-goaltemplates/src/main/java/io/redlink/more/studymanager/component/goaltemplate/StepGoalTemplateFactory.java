package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.measurement.Measurement;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.BooleanValue;
import io.redlink.more.studymanager.core.properties.model.IntegerRange;
import io.redlink.more.studymanager.core.properties.model.IntegerRangeValue;
import io.redlink.more.studymanager.core.properties.model.StringListValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

import java.util.List;
import java.util.Set;


public class StepGoalTemplateFactory extends GoalTemplateFactory<StepGoalTemplate<GoalTemplateProperties>, GoalTemplateProperties> {

    public StepGoalTemplateFactory() { }

    public static final String FIELD_TARGET_STEPS =  "targetSteps";
    public static final String FIELD_STEPS =  "steps";
    public static final String FIELD_QUESTION =  "question";
    public static final String FIELD_ANSWER =  "answer";
    public static final String FIELD_TARGET_DAYS_IN_WEEK =  "targetDays";

    protected static final String STEP_PROPERTY_PREFIX = GOAL_TEMPLATE_PROPERTY_PREFIX + "steps.";


    private static final MeasurementSet measurements = new MeasurementSet(
            "SELF_ASSESSMENT", Set.of(
            new Measurement(FIELD_GOAL_KIND, Measurement.Type.STRING),
            new Measurement(FIELD_GOAL_CATEGORY, Measurement.Type.STRING),
            new Measurement(FIELD_QUESTION, Measurement.Type.STRING),
            new Measurement(FIELD_ANSWER, Measurement.Type.STRING),
            new Measurement(FIELD_TARGET_STEPS, Measurement.Type.INTEGER),
            new Measurement(FIELD_STEPS, Measurement.Type.INTEGER),
            new Measurement(FIELD_TARGET_DAYS_IN_WEEK, Measurement.Type.INTEGER)));


    @Override
    public String getId() {
        return "steps";
    }

    @Override
    public MeasurementSet getMeasurementSet() {
        return measurements;
    }

    public List<Value> getProperties() {
        return List.of(
                CONFIG_SECTION_CONFIGURATION,
                //GOAL_TITLE_STATE, -> title can not be modified by user!
                //ALLOW_INSTANCES_STATE -> not multiple instances of boolean goals
                //CUSTOM_ADHERENCE_CHECKS_STATE, //allow to enable/disable custom adherence checks for multiple checks per day
                //ADHERENCE_CHECK_BASED_SCHEDULE_STATE, only once a day in the evening
                BASELINE_TRACKING_STATE, //allow to configure baseline tracking for boolean goals (default enabled)

                CONFIG_SECTION_GOAL_CONFIGURATION,
                new IntegerRangeValue( "steps.stepRange")
                        .setMin(1)
                        .setMax(Integer.MAX_VALUE)
                        .setName(STEP_PROPERTY_PREFIX + "goal.stepRange.name")
                        .setDescription(STEP_PROPERTY_PREFIX + "goal.stepRange.description")
                        .setDefaultValue(new IntegerRange(500, 10000))
                        .setRequired(true),
                DAYS_OF_WEEK,

                CONFIG_SECTION_STATUS,
                STATUS_100_PERCENT_REACHED,
                STATUS_75_PERCENT_REACHED.copyOf()
                        .setDefaultValue("Dein Ziel ist zum greifen nah. Noch ein kleiner Spaziergang und Du hast es geschafft!"),
                STATUS_0_PERCENT_REACHED.copyOf()
                        .setDefaultValue("Du bist auf dem richtigen Weg. Jede Schritt macht Dich stärker."),


                CONFIG_SECTION_SELF_REPORT,
                new StringValue("question")
                        .setName(STEP_PROPERTY_PREFIX + "goal.question.name")
                        .setDescription(STEP_PROPERTY_PREFIX + "goal.question.description")
                        .setRequired(false),
                new StringListValue("answers")
                        .setMinSize(2)
                        .setMaxSize(10)
                        .setName(STEP_PROPERTY_PREFIX + "goal.answer.name")
                        .setDescription(STEP_PROPERTY_PREFIX + "goal.answer.description")
                        .setDefaultValue(List.of(
                                "No",
                                "Yes"
                        ))
                        .setRequired(false),
                new BooleanValue("singleChoiceState")
                        .setName(STEP_PROPERTY_PREFIX + "goal.singleChoiceState.name")
                        .setDescription(STEP_PROPERTY_PREFIX + "goal.singleChoiceState.description")
                        .setRequired(false)
                );
    }

    @Override
    public Class<GoalTemplateProperties> getPropertyClass() {
        return GoalTemplateProperties.class;
    }

    @Override
    public StepGoalTemplate<GoalTemplateProperties> create(MoreGoalTemplateSDK sdk, GoalTemplateProperties properties) throws ConfigurationValidationException {
        return new StepGoalTemplate<>(sdk, properties);
    }

}
