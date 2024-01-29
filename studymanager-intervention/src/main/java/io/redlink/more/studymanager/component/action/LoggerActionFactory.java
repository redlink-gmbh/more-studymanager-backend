package io.redlink.more.studymanager.component.action;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.properties.ActionProperties;
import io.redlink.more.studymanager.core.sdk.MoreActionSDK;

public class LoggerActionFactory extends ActionFactory<LoggerAction, ActionProperties> {
    @Override
    public LoggerAction create(MoreActionSDK sdk, ActionProperties properties) throws ConfigurationValidationException {
        return new LoggerAction(sdk, properties);
    }

    @Override
    public String getId() {
        return "logger-action";
    }

    @Override
    public String getTitle() {
        return "Logger Action";
    }

    @Override
    public String getDescription() {
        return "Logs to info level";
    }
}
