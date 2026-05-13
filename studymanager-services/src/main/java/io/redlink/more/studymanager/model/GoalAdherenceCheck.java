package io.redlink.more.studymanager.model;

import java.time.LocalTime;
import java.util.Objects;

public class GoalAdherenceCheck {
    private Long studyId;
    private Integer checkId;
    private String title;
    private LocalTime time;

    public Long getStudyId() {
        return studyId;
    }

    public GoalAdherenceCheck setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public Integer getCheckId() {
        return checkId;
    }

    public GoalAdherenceCheck setCheckId(Integer checkId) {
        this.checkId = checkId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public GoalAdherenceCheck setTitle(String title) {
        this.title = title;
        return this;
    }

    public LocalTime getTime() {
        return time;
    }

    public GoalAdherenceCheck setTime(LocalTime time) {
        this.time = time;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GoalAdherenceCheck that = (GoalAdherenceCheck) o;
        return Objects.equals(studyId, that.studyId) && Objects.equals(checkId, that.checkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studyId, checkId);
    }
}