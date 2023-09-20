package io.redlink.more.studymanager.component.action;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.properties.ActionProperties;
import io.redlink.more.studymanager.core.properties.model.*;
import io.redlink.more.studymanager.core.sdk.MoreActionSDK;

import java.util.List;

public class PushNotificationActionFactory extends ActionFactory<PushNotificationAction, ActionProperties> {

    private static List<Value> properties = List.of(
            new StringValue("title")
                    .setRequired(true),
            new StringTextValue("message")
                    .setRequired(true)
    );
    @Override
    public PushNotificationAction create(MoreActionSDK sdk, ActionProperties properties) throws ConfigurationValidationException {
        return new PushNotificationAction(sdk, validate(properties));
    }

    @Override
    public String getId() {
        return "push-notification-action";
    }

    @Override
    public String getTitle() {
        return "intervention.factory.actions.pushNotification.title";
    }

    @Override
    public String getDescription() {
        return "intervention.factory.actions.pushNotification.description";
    }

    @Override
    public List<Value> getProperties() {
        return properties;
    }
}
