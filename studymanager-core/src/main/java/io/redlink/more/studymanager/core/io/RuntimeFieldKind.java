/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.io;

/**
 * The type a source data field is read as by a {@link RuntimeFieldSpec}. Values that cannot be read as this type are
 * skipped, so they never match a query on the runtime field.
 */
public enum RuntimeFieldKind {
    BOOLEAN,
    LONG,
    DOUBLE
}
