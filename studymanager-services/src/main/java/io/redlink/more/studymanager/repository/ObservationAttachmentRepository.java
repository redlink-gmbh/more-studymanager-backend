/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.model.ObservationAttachment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ObservationAttachmentRepository {

    private static final String INSERT =
            "INSERT INTO observation_attachments (attachment_id, study_id, observation_id, filename, content_type, size_bytes) " +
                    "VALUES (:attachment_id, :study_id, :observation_id, :filename, :content_type, :size_bytes) RETURNING *";
    private static final String GET =
            "SELECT * FROM observation_attachments WHERE study_id = :study_id AND observation_id = :observation_id AND attachment_id = :attachment_id";
    private static final String LIST =
            "SELECT * FROM observation_attachments WHERE study_id = :study_id AND observation_id = :observation_id ORDER BY created";
    private static final String DELETE =
            "DELETE FROM observation_attachments WHERE study_id = :study_id AND observation_id = :observation_id AND attachment_id = :attachment_id";

    private final NamedParameterJdbcTemplate namedTemplate;

    public ObservationAttachmentRepository(NamedParameterJdbcTemplate namedTemplate) {
        this.namedTemplate = namedTemplate;
    }

    public ObservationAttachment insert(long studyId, int observationId, UUID attachmentId, String filename,
                                        String contentType, long size) {
        return namedTemplate.queryForObject(INSERT, params(studyId, observationId, attachmentId)
                .addValue("filename", filename)
                .addValue("content_type", contentType)
                .addValue("size_bytes", size), ROW_MAPPER);
    }

    public List<ObservationAttachment> list(long studyId, int observationId) {
        return namedTemplate.query(LIST, new MapSqlParameterSource()
                .addValue("study_id", studyId)
                .addValue("observation_id", observationId), ROW_MAPPER);
    }

    public Optional<ObservationAttachment> get(long studyId, int observationId, UUID attachmentId) {
        try (var stream = namedTemplate.queryForStream(GET, params(studyId, observationId, attachmentId), ROW_MAPPER)) {
            return stream.findFirst();
        }
    }

    public void delete(long studyId, int observationId, UUID attachmentId) {
        namedTemplate.update(DELETE, params(studyId, observationId, attachmentId));
    }

    private static MapSqlParameterSource params(long studyId, int observationId, UUID attachmentId) {
        return new MapSqlParameterSource()
                .addValue("study_id", studyId)
                .addValue("observation_id", observationId)
                .addValue("attachment_id", attachmentId);
    }

    private static final RowMapper<ObservationAttachment> ROW_MAPPER = (rs, rowNum) -> new ObservationAttachment(
            rs.getObject("attachment_id", UUID.class),
            rs.getLong("study_id"),
            rs.getInt("observation_id"),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            RepositoryUtils.readInstant(rs, "created")
    );
}
