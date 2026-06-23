package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.measurement.Measurement;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.ConfigSection;
import io.redlink.more.studymanager.core.properties.model.IntegerRange;
import io.redlink.more.studymanager.core.properties.model.IntegerRangeValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.properties.model.ValueGroup;

import java.util.Objects;
import java.util.Set;

public abstract class AbstractAmountOfGoalTemplateFactory<C extends GoalTemplate<P>, P extends GoalTemplateProperties> extends GoalTemplateFactory<C, P> {

    public static final String FIELD_AMOUNT =  "amount";
    public static final String FIELD_UNIT =  "unit";
    public static final String FIELD_TARGET_AMOUNT =  "targetAmount";
    public static final String FIELD_TARGET_DAYS_IN_WEEK =  "targetDays";

    private static final MeasurementSet measurements = new MeasurementSet(
            "SELF_ASSESSMENT", Set.of(
                    new Measurement(FIELD_GOAL_KIND, Measurement.Type.STRING),
                    new Measurement(FIELD_GOAL_CATEGORY, Measurement.Type.STRING),
                    new Measurement(FIELD_AMOUNT, Measurement.Type.INTEGER),
                    new Measurement(FIELD_UNIT, Measurement.Type.STRING),
                    new Measurement(FIELD_TARGET_AMOUNT, Measurement.Type.INTEGER),
                    new Measurement(FIELD_TARGET_DAYS_IN_WEEK, Measurement.Type.INTEGER)));

    protected static final String AMOUNT_OF_PROPERTY_PREFIX = GOAL_TEMPLATE_PROPERTY_PREFIX + "amountOfGoal.";

    protected static final Value<Void> CONFIGS_GOAL_AMOUNT_GROUP = new ValueGroup("goal")
            .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal.name")
            .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal.description");
    protected static final Value<IntegerRange> CONFIGS_GOAL_AMOUNT_VALUE = new IntegerRangeValue(CONFIGS_GOAL_AMOUNT_GROUP.getId() + ".amount")
            .setMin(1)
            .setMax(Integer.MAX_VALUE)
            .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal.amount.name")
            .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal.amount.description")
            .setRequired(true);
    protected static final Value<String> CONFIGS_GOAL_AMOUNT_UNIT = new StringValue(CONFIGS_GOAL_AMOUNT_GROUP.getId() + ".unit")
            .setName(AMOUNT_OF_PROPERTY_PREFIX + "goal.unit.name")
            .setDescription(AMOUNT_OF_PROPERTY_PREFIX + "goal.unit.description")
            .setRequired(true);

    protected final String id;

    AbstractAmountOfGoalTemplateFactory(String id){
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
