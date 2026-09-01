/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

import java.util.ArrayList;
import java.util.List;

public class ChoiceValue extends Value<String> {
    private List<String> options = new ArrayList<>();

    public ChoiceValue(String id) {
        super(id);
    }

    public List<String> getOptions() {
        return options;
    }

    public ChoiceValue setOptions(List<String> options) {
        this.options = options == null ? new ArrayList<>() : options;
        return this;
    }

    @Override
    public String getType() {
        return "CHOICE";
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    @Override
    public Value<String> clone() {
        return copyState(new ChoiceValue(getId())
                .setOptions(getOptions()));
    }
}
