package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.measurement.Measurement;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.BooleanValue;
import io.redlink.more.studymanager.core.properties.model.ConfigSection;
import io.redlink.more.studymanager.core.properties.model.IntegerRange;
import io.redlink.more.studymanager.core.properties.model.IntegerRangeValue;
import io.redlink.more.studymanager.core.properties.model.StringTextValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.properties.model.ValueGroup;

import java.util.Objects;
import java.util.Set;

public abstract class AbstractBooleanGoalTemplateFactory<C extends GoalTemplate<P>, P extends GoalTemplateProperties> extends GoalTemplateFactory<C, P> {

    public static final String FIELD_QUESTION =  "question";
    public static final String FIELD_STATE =  "state";
    public static final String FIELD_DESIRED =  "desired";
    public static final String FIELD_TARGET_DAYS_IN_WEEK =  "targetDays";

    private static final MeasurementSet measurements = new MeasurementSet(
            "SELF_ASSESSMENT", Set.of(
                    new Measurement(FIELD_GOAL_KIND, Measurement.Type.STRING),
                    new Measurement(FIELD_GOAL_CATEGORY, Measurement.Type.STRING),
                    new Measurement(FIELD_QUESTION, Measurement.Type.STRING),
                    new Measurement(FIELD_STATE, Measurement.Type.BOOLEAN),
                    new Measurement(FIELD_DESIRED, Measurement.Type.BOOLEAN),
                    new Measurement(FIELD_TARGET_DAYS_IN_WEEK, Measurement.Type.INTEGER)));

    protected static final String BOOLEAN_PROPERTY_PREFIX = GOAL_TEMPLATE_PROPERTY_PREFIX + "boolean.";

    protected static final Value<Boolean> CONFIGS_GOAL_DESIRED = new BooleanValue("goal.desired")
            .setName(BOOLEAN_PROPERTY_PREFIX + "goal.desired.name")
            .setDescription(BOOLEAN_PROPERTY_PREFIX + "goal.desired.description")
            .setDefaultValue(true)
            .setRequired(true);

    protected static final Value<String> CONFIGS_GOAL_QUESTION = new StringTextValue("goal.question")
            .setName(BOOLEAN_PROPERTY_PREFIX + "goal.question.name")
            .setDescription(BOOLEAN_PROPERTY_PREFIX + "goal.question.description")
            .setRequired(true);


    protected final String id;

    AbstractBooleanGoalTemplateFactory(String id){
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final MeasurementSet getMeasurementSet() {
        return measurements;
    }

}
