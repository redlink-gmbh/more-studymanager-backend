package io.redlink.more.studymanager.core.properties.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class IntegerRange {

    // Defines the lower set value of the range
    private final int lower;
    // Defines the upper set value of the range
    private final int upper;

    @JsonCreator
    public IntegerRange(
            @JsonProperty("lower") @JsonAlias({"min"}) int lower,
            @JsonProperty("upper") @JsonAlias({"max"}) int upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public int getLower() {
        return lower;
    }

    public int getUpper() {
        return upper;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IntegerRange that = (IntegerRange) o;
        return lower == that.lower && upper == that.upper;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper);
    }

    @Override
    public String toString() {
        return "IntegerRange{" +
                "lower=" + lower +
                ", upper=" + upper +
                '}';
    }
}
