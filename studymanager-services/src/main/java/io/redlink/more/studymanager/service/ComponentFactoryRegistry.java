package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.factory.ComponentFactory;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.factory.TriggerFactory;
import io.redlink.more.studymanager.model.Observation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class ComponentFactoryRegistry {

    private final ApplicationContext applicationContext;

    public ComponentFactoryRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Set<String> getObservationFactories(){
        return getComponentFactories(ObservationFactory.class);
    }

    public Optional<ObservationFactory> getObservationFactory(String observationType) {
        return getComponentFactory(observationType, ObservationFactory.class);
    }

    public Set<String> getTriggerFactories(){
        return getComponentFactories(TriggerFactory.class);
    }

    public Optional<TriggerFactory> getTriggerFactory(String triggerType) {
        return getComponentFactory(triggerType, TriggerFactory.class);
    }

    public Set<String> getActionFactories(){
        return getComponentFactories(ActionFactory.class);
    }

    public Optional<ActionFactory> getActionFactory(String actionType) {
        return getComponentFactory(actionType, ActionFactory.class);
    }

    public <T extends ComponentFactory<?,?>> Set<String> getComponentFactories(Class<T> type){
        return Set.of(applicationContext.getBeanNamesForType(type, false, false));
    }


    public <T extends ComponentFactory<?,?>> Optional<T> getComponentFactory(String name, Class<T> type) {
        try {
            return Optional.of(applicationContext.getBean(type, type));
        }  catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        } //do not catch other BeansExceptions
    }

}
