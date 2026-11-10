/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.component.trigger.datacheck;

import com.fasterxml.jackson.core.type.TypeReference;
import io.redlink.more.studymanager.core.properties.TriggerProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScheduledDatacheckTriggerProperties extends TriggerProperties {
    public ScheduledDatacheckTriggerProperties(TriggerProperties triggerProperties) {
        super(triggerProperties);
    }

    public Optional<String> getCronSchedule() {
        return Optional.ofNullable(this.getString("cronSchedule"));
    }

    public Optional<List<QueryObject>> getQueryObject() {
        return this.getObject("queryObject", new TypeReference<>() {
        });
    }

    public Optional<Long> getWindow() {
        return Optional.ofNullable(this.getLong("window"));
    }

    public Optional<Boolean> getOnlyOnce() {
        return Optional.ofNullable(this.getBoolean("onlyOnce"));
    }

    /**
     * Runtime fields (name &rarr; source field) required by the conditions of {@link #getElasticQueryString()}.
     */
    public Map<String, String> getRuntimeFields() {
        return this.getQueryObject().stream()
                .flatMap(List::stream)
                .flatMap(qo -> qo.parameter.stream())
                .flatMap(p -> p.getRuntimeField().stream())
                .collect(Collectors.toMap(DataPointQuery.RuntimeField::name, DataPointQuery.RuntimeField::sourceField, (a, b) -> a));
    }

    public Optional<String> getElasticQueryString() {
        return this.getQueryObject().map(o -> o.stream()
                .map(qo ->
                        qo.toQueryString() + (qo.nextGroupCondition != null ? " " + qo.nextGroupCondition.value() : ""))
                .collect(Collectors.joining(" ")));
    }
}
