/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.factory;

import io.redlink.more.studymanager.core.component.GoalTemplate;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.io.Visibility;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.model.BooleanValue;
import io.redlink.more.studymanager.core.properties.model.IntegerRange;
import io.redlink.more.studymanager.core.properties.model.IntegerRangeValue;
import io.redlink.more.studymanager.core.properties.model.StringTextValue;
import io.redlink.more.studymanager.core.properties.model.StringValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreGoalTemplateSDK;

public abstract class GoalTemplateFactory<C extends GoalTemplate<P>, P extends GoalTemplateProperties> extends ComponentFactory<C, P> {

    private static final Visibility GOAL_TEMPLATE_VISIBILITY = new Visibility(false, false);

    public abstract C create(MoreGoalTemplateSDK sdk, P properties) throws ConfigurationValidationException;

    @Override
    public Class<GoalTemplateProperties> getPropertyClass() {
        return GoalTemplateProperties.class;
    }

    public abstract MeasurementSet getMeasurementSet();

    public final Visibility getVisibility() {
        return GOAL_TEMPLATE_VISIBILITY;
    }

    /**
     * Getter for the propertiy to internationalize the title
     * (<pre>{@link #GOAL_TEMPLATE_FACTORY_PREFIX} + {@link #getId()} + ".name"</pre>)
     * @return the internationalisation property for the title
     */
    public final String getTitle() {
        return GOAL_TEMPLATE_FACTORY_PREFIX + getId() + ".name";
    }

    /**
     * Getter for the propertiy to internationalize the description
     * (<pre>{@link #GOAL_TEMPLATE_FACTORY_PREFIX} + {@link #getId()} + ".description"</pre>)
     * @return the internationalisation property for the description
     */
    public final String getDescription() {
        return GOAL_TEMPLATE_FACTORY_PREFIX + getId() + ".description";
    }

    //Constants

    /**
     * The kind of the goal (outcome, behavioral)
     */
    public static final String FIELD_GOAL_KIND =  "goal-kind";
    /**
     * The key of the goal category
     */
    public static final String FIELD_GOAL_CATEGORY =  "goal-category";


    /**
     * Prefix to be used for all goal template related configurations
     */
    public static final String GOAL_TEMPLATE_PREFIX = "goaltemplate.";
    /**
     * Prefix to be used for all goal template factory related configurations (e.g. title, description)
     */
    public static final String GOAL_TEMPLATE_FACTORY_PREFIX = "goaltemplate.factory.";

    /**
     * Property prefix to be used for all goal template properties ('<pre>goaltemplate.property.</pre>')
     */
    public static final String GOAL_TEMPLATE_PROPERTY_PREFIX = "goaltemplate.property.";
    /**
     * GoalTemplate property prefix used by all globally defined properties
     */
    private static final String GLOBAL_PROPERTY_PREFIX = GOAL_TEMPLATE_PROPERTY_PREFIX + "global.";

    /**
     * Used to configure the title of the GoalTemplate as shown in the application
     */
    public static final Value<String> APP_TITLE = new StringValue("app-title")
            .setName(GLOBAL_PROPERTY_PREFIX + "appTitle.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "appTitle.description")
            .setRequired(true);
    /**
     * Used to configure the custom description of the GoalTemplate as shown in the application
     */
    public static final Value<String> APP_DESCRIPTION = new StringTextValue("app-description")
            .setName(GLOBAL_PROPERTY_PREFIX + "appDescription.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "appDescription.description")
            .setRequired(false);


    /**
     * Allows to define that a goal is aktive on x days of the week (e.g. 5/7 days)
     */
    public static final Value<IntegerRange> DAYS_OF_WEEK = new IntegerRangeValue("days-of-week")
            .setMin(1) //at least at one day of the week
            .setMax(7) //a week has only 7 days
            .setName(GLOBAL_PROPERTY_PREFIX + "days-of-week.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "days-of-week.description")
            .setDefaultValue(new IntegerRange(7, 7));

    /**
     * User can assign a custom title to the GoalTemplate
     */
    public static final Value<Boolean> GOAL_TITLE_STATE = new BooleanValue("goal-title-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "goalTitleState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "goalTitleState.description")
            .setDefaultValue(false);
    /**
     * User can create multiple instance of the same GoalTemplate
     */
    public static final Value<Boolean> ALLOW_INSTANCES_STATE = new BooleanValue("instance-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "instanceState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "instanceState.description")
            .setDefaultValue(false);

    /**
     * If enabled the participant can select adherence checks for the goal. If missing/disabled the adherence checks
     * as defined by the template are used.
     */
    public static final Value<Boolean> CUSTOM_ADHERENCE_CHECKS_STATE = new BooleanValue("custom-adherence-checks-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "customAdherenceChecksState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "customAdherenceChecksState.description")
            .setDefaultValue(false);

    /**
     * If enabled the schedule for a goal is calculated based on the actuve adherence checks. If disabled the goal is
     * active the whole day.
     */
    public static final Value<Boolean> ADHERENCE_CHECK_BASED_SCHEDULE_STATE = new BooleanValue("adherence-check-schedule-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "adherenceChecksScheduleState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "adherenceChecksScheduleState.description")
            .setDefaultValue(false);

    /**
     * If enabled the activity of the goal is shown as a to do itme in the praecura app. This allows interacting with
     * the goal step directly via the today view
     */
    public static final Value<Boolean> SHOW_AS_TODO_ITEM_STATE = new BooleanValue("show-as-todo-item-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "showAsTodoItemState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "showAsTodoItemState.description")
            .setDefaultValue(false);
    /**
     * If enabled the participant can activate/disactivate if the goal is included in the to do item list. The
     * <code>show-as-todo-item-state</code> is used as a default
     */
    public static final Value<Boolean> CUSTOM_SHOW_AS_TODO_ITEM_STATE = new BooleanValue("custom-show-as-todo-item-state")
            .setName(GLOBAL_PROPERTY_PREFIX + "customShowAsTodoItemState.name")
            .setDescription(GLOBAL_PROPERTY_PREFIX + "customShowAsTodoItemState.description")
            .setDefaultValue(false);

}
