/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.model;

import java.time.Instant;
import java.util.UUID;

public record ObservationAttachment(
        UUID attachmentId,
        long studyId,
        int observationId,
        String filename,
        String contentType,
        long size,
        Instant created
) {
}
