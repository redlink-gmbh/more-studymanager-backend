package io.redlink.more.studymanager.model;

public record StudyComponent(
    Long studyId,
    Integer componentId,
    String type,
    String title
) {}
