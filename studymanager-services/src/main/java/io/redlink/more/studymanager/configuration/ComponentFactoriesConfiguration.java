/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.configuration;

import io.redlink.more.studymanager.core.factory.ActionFactory;
import io.redlink.more.studymanager.core.factory.ComponentFactory;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.factory.TriggerFactory;
import io.redlink.more.studymanager.properties.ComponentFactoriesProperties;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties({ComponentFactoriesProperties.class})
public class ComponentFactoriesConfiguration implements BeanFactoryAware {
    private final Logger logger = LoggerFactory.getLogger(ComponentFactoriesConfiguration.class);
    private ConfigurableBeanFactory beanFactory;

    private final Reflections reflections;
    private final ComponentFactoriesProperties componentFactoriesProperties;

    public ComponentFactoriesConfiguration(ComponentFactoriesProperties componentFactoriesProperties) {
        this.reflections = new Reflections("io.redlink.more.studymanager.component");
        this.componentFactoriesProperties = componentFactoriesProperties;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @PostConstruct
    public void onPostConstruct() {

        initAndRegisterFactory(getFactoryImplementations(ObservationFactory.class));
        initAndRegisterFactory(getFactoryImplementations(TriggerFactory.class));
        initAndRegisterFactory(getFactoryImplementations(ActionFactory.class));
        initAndRegisterFactory(getFactoryImplementations(GoalTemplateFactory.class));
    }

    private <T extends ComponentFactory<?, ?>> void initAndRegisterFactory(Stream<Class<? extends T>> factories) {
        factories
                .map(this::instantiate)
                .map(f -> f.init(componentFactoriesProperties.get(f.getId())))
                .forEach(m -> {
                    logger.trace("Registering ComponentFactory: {} [class:{}, properties:{}]", m.getId(), m.getClass().getName(), m.getProperties());
                    beanFactory.registerSingleton(m.getId(), m);
                });
    }

    private <T> Stream<Class<? extends T>> getFactoryImplementations(Class<T> factoryType) {
        return reflections.getSubTypesOf(factoryType)
                .stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers()) && !c.isInterface());
    }

    private <T> T instantiate(Class<? extends T> c) {
        try {
            return c.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
