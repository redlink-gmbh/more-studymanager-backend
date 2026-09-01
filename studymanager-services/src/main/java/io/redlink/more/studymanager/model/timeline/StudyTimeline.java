/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.model.timeline;

import org.apache.commons.lang3.Range;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudyTimeline(
        Instant signup,
        Range<LocalDate> participationRange,
        List<ObservationTimelineEvent> observationTimelineEvents,
        List<InterventionTimelineEvent> interventionTimelineEvents
) {
}
