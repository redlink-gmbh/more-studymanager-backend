package io.redlink.more.studymanager.core.properties.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public class NamedIntegerRange extends IntegerRange {

    private String name;

    @JsonCreator
    public NamedIntegerRange(String name, int lower, int upper) {
        super(lower, upper);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NamedIntegerRange that = (NamedIntegerRange) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "NamedIntegerRange{" +
                "name='" + name + '\'' +
                "lower=" + getLower() +
                ", upper=" + getUpper() +
                '}';
    }
}
