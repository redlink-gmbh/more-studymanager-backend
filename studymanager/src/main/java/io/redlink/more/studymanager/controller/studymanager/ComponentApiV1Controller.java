package io.redlink.more.studymanager.controller.studymanager;

import com.fasterxml.jackson.databind.JsonNode;
import io.redlink.more.studymanager.api.v1.model.ComponentFactoryDTO;
import io.redlink.more.studymanager.api.v1.model.ValidationReportDTO;
import io.redlink.more.studymanager.api.v1.model.ValidationReportItemDTO;
import io.redlink.more.studymanager.api.v1.webservices.ComponentsApi;
import io.redlink.more.studymanager.core.exception.ApiCallException;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.factory.ComponentFactory;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.factory.TriggerFactory;
import io.redlink.more.studymanager.core.model.User;
import io.redlink.more.studymanager.core.properties.ComponentProperties;
import io.redlink.more.studymanager.core.webcomponent.WebComponent;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import io.redlink.more.studymanager.utils.MapperUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ComponentApiV1Controller implements ComponentsApi {

    private final Map<String, ObservationFactory> observationFactories;
    private final Map<String, TriggerFactory> triggertFactories;
    private final Map<String, ActionFactory> actionFactories;
    private final OAuth2AuthenticationService authService;

    public ComponentApiV1Controller(
            Map<String, ObservationFactory> observationFactories,
            Map<String, TriggerFactory> triggertFactories,
            Map<String, ActionFactory> actionFactories,
            OAuth2AuthenticationService authService
    ) {
        this.observationFactories = observationFactories;
        this.triggertFactories = triggertFactories;
        this.actionFactories = actionFactories;
        this.authService = authService;
    }

    @Override
    public ResponseEntity<List<ComponentFactoryDTO>> listComponents(String componentType) {
        return switch (componentType) {
            case "observation" -> ResponseEntity.ok(observationFactories.values().stream().map(this::toComponentDTO).toList());
            case "trigger" -> ResponseEntity.ok(triggertFactories.values().stream().map(this::toComponentDTO).toList());
            case "action" -> ResponseEntity.ok(actionFactories.values().stream().map(this::toComponentDTO).toList());
            default -> ResponseEntity.notFound().build();
        };
    }

    @Override
    public ResponseEntity<ValidationReportDTO> validateProperties(String componentType, String componentId, Object body) {
        return getComponentFactory(componentType, componentId)
                .map(f -> {
                    try {
                        f.validate((ComponentProperties) MapperUtils.MAPPER.convertValue(body, f.getPropertyClass()));
                        return new ValidationReportDTO().valid(true);
                    } catch (ConfigurationValidationException e) {
                        return new ValidationReportDTO()
                                .valid(false)
                                .errors(e.getReport().getErrors().stream()
                                        .map(i -> new ValidationReportItemDTO().message(i.getMessage()).propertyId(i.getPropertyId()).type("error"))
                                        .toList()
                                ).warnings(e.getReport().getWarnings().stream()
                                        .map(i -> new ValidationReportItemDTO().message(i.getMessage()).propertyId(i.getPropertyId()).type("warning"))
                                        .toList()
                                );
                    }
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Object> accessModuleSpecificEndpoint(String componentType, String componentId, String slug, Object body) {
        Optional<ComponentFactory> componentFactory = getComponentFactory(componentType, componentId);
        if (componentFactory.isPresent()) {
            try {
                JsonNode jsonNodeBody = MapperUtils.MAPPER.valueToTree(body);
                return ResponseEntity.ok(componentFactory.get().handleAPICall(slug, new User(authService.getCurrentUser().email()), jsonNodeBody));
            } catch (ApiCallException e) {
                throw new ResponseStatusException(e.getStatus(), e.getMessage(), null);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<String> getWebComponentScript(String componentType, String componentId) {
        return getComponentFactory(componentType, componentId)
                .map(f -> getWebComponentScript(f, componentId))
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<ComponentFactory> getComponentFactory(String componentType, String componentId) {
        return switch (componentType) {
            case "observation" -> getComponentFactory(observationFactories, componentId);
            case "trigger" -> getComponentFactory(triggertFactories, componentId);
            case "action" -> getComponentFactory(actionFactories, componentId);
            default -> Optional.empty();
        };
    }

    private Optional<ComponentFactory> getComponentFactory(Map<String, ? extends ComponentFactory> factories, String componentId) {
        return Optional.ofNullable(factories.get(componentId));
    }

    private ResponseEntity<String> getWebComponentScript(ComponentFactory factory, String componentId) {
        if(factory.hasWebComponent()) {
            return ResponseEntity.ok(
                    toScript(componentId, factory.getWebComponent())
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String toScript(String componentId, WebComponent webComponent) {
        return webComponent.getScript() + "\n" +
                String.format(
                        "customElements.define( 'webcomponent-%s', %s );",
                        componentId,
                        webComponent.getClassName()
                );
    }

    private ComponentFactoryDTO toComponentDTO(ComponentFactory factory) {
        return new ComponentFactoryDTO()
                .componentId(factory.getId())
                .title(factory.getTitle())
                .properties(factory.getProperties())
                .defaultProperties(factory.getDefaultProperties())
                .description(factory.getDescription())
                .hasWebComponent(factory.hasWebComponent());
    }
}
