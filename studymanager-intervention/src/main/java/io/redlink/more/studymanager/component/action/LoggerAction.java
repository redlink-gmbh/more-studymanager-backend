package io.redlink.more.studymanager.component.action;

import io.redlink.more.studymanager.core.component.Action;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.io.ActionParameter;
import io.redlink.more.studymanager.core.properties.ActionProperties;
import io.redlink.more.studymanager.core.sdk.MoreActionSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerAction extends Action<ActionProperties> {

    protected LoggerAction(MoreActionSDK sdk, ActionProperties properties) throws ConfigurationValidationException {
        super(sdk, properties);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerAction.class);
    @Override
    public void execute(ActionParameter parameter) {
        LOGGER.info(parameter.toString());
    }
}
