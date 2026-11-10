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
 * A field computed at query time from a stored data field.
 *
 * @param target name of the runtime field used in the query (e.g. {@code long_data_score})
 * @param source name of the stored data field it is read from (e.g. {@code data_score})
 * @param kind   the type the source value is read as
 */
public record RuntimeFieldSpec(String target, String source, RuntimeFieldKind kind) {
}
