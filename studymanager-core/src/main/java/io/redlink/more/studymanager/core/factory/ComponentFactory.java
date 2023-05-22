package io.redlink.more.studymanager.core.factory;

import com.fasterxml.jackson.databind.JsonNode;
import io.redlink.more.studymanager.core.component.Component;
import io.redlink.more.studymanager.core.exception.ApiCallException;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.exception.ValueCastException;
import io.redlink.more.studymanager.core.exception.ValueNonNullException;
import io.redlink.more.studymanager.core.model.User;
import io.redlink.more.studymanager.core.properties.ComponentProperties;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.validation.ConfigurationValidationReport;
import io.redlink.more.studymanager.core.validation.ValidationIssue;
import io.redlink.more.studymanager.core.webcomponent.WebComponent;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ComponentFactory<C extends Component, P extends ComponentProperties> {
    public ComponentFactoryProperties componentProperties;
    public ComponentFactory<C,P> init(ComponentFactoryProperties componentProperties){
        this.componentProperties = componentProperties;
        return this;
    }
    public abstract String getId();

    public abstract String getTitle();

    public List<Value> getProperties() {
        return List.of();
    }

    public abstract <P extends  ComponentProperties> Class<P> getPropertyClass();

    public abstract String getDescription();

    public P validate(P values) {
        try {
            ConfigurationValidationReport report = ConfigurationValidationReport.of(
                    getProperties().stream()
                            .map(p -> p.validate(p.getValue(values)))
                            .filter(ValidationIssue::nonNone)
                            .collect(Collectors.toList())
            );

            if(report.isValid()) {
                return values;
            } else {
                throw new ConfigurationValidationException(report);
            }
        } catch (ValueCastException | ValueNonNullException e) {
            throw new ConfigurationValidationException(
                    ConfigurationValidationReport.of(ValidationIssue.error(
                            e.getValue(),
                            e.getMessage()
                    ))
            );
        }
    }

    public WebComponent getWebComponent() {
        return null;
    }

    public JsonNode handleAPICall(String slug, User user, JsonNode input) throws ApiCallException {
        throw new ApiCallException(404, "Not found");
    }

    public boolean hasWebComponent() {
        return getWebComponent() != null;
    }

}
