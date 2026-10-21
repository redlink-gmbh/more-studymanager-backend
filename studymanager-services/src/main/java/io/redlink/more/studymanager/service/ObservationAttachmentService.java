/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.ObservationAttachment;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.repository.ObservationAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Files attached to observations. Metadata is kept in the database, the content on the filesystem at
 * {@code {dir}/{studyId}/{observationId}/{attachmentId}} (paths are built from ids only, never from user input).
 * <p>
 * ponytail: uploaded files no longer referenced by any message are kept until the observation is deleted;
 * add a cleanup job if storage becomes an issue.
 */
@Service
public class ObservationAttachmentService {

    private static final Logger LOG = LoggerFactory.getLogger(ObservationAttachmentService.class);

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final ObservationAttachmentRepository repository;
    private final StudyStateService studyStateService;
    private final Path baseDir;

    public ObservationAttachmentService(ObservationAttachmentRepository repository,
                                        StudyStateService studyStateService,
                                        @Value("${more.attachments.dir:./data/attachments}") Path baseDir) {
        this.repository = repository;
        this.studyStateService = studyStateService;
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    public ObservationAttachment addAttachment(long studyId, int observationId, String filename, String contentType,
                                               long size, InputStream content) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        if (size <= 0 || size > MAX_SIZE_BYTES) {
            throw new BadRequestException("Attachment must not be empty or larger than 10 MB");
        }
        UUID id = UUID.randomUUID();
        Path file = file(studyId, observationId, id);
        try {
            Files.createDirectories(file.getParent());
            Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING);
            return repository.insert(studyId, observationId, id,
                    filename == null || filename.isBlank() ? id.toString() : filename,
                    contentType == null ? "application/octet-stream" : contentType,
                    Files.size(file));
        } catch (DataIntegrityViolationException e) {
            deleteQuietly(file);
            throw NotFoundException.Observation(studyId, observationId);
        } catch (IOException e) {
            deleteQuietly(file);
            throw new UncheckedIOException(e);
        }
    }

    public List<ObservationAttachment> listAttachments(long studyId, int observationId) {
        return repository.list(studyId, observationId);
    }

    public Optional<ObservationAttachment> getAttachment(long studyId, int observationId, UUID attachmentId) {
        return repository.get(studyId, observationId, attachmentId);
    }

    public Path getFile(ObservationAttachment attachment) {
        return file(attachment.studyId(), attachment.observationId(), attachment.attachmentId());
    }

    public void deleteAttachment(long studyId, int observationId, UUID attachmentId) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        repository.delete(studyId, observationId, attachmentId);
        deleteQuietly(file(studyId, observationId, attachmentId));
    }

    /** Removes all files of an observation (metadata is removed by the FK cascade). */
    public void deleteFiles(long studyId, int observationId) {
        deleteDirQuietly(baseDir.resolve(String.valueOf(studyId)).resolve(String.valueOf(observationId)));
    }

    /** Removes all files of a study (metadata is removed by the FK cascade). */
    public void deleteFiles(long studyId) {
        deleteDirQuietly(baseDir.resolve(String.valueOf(studyId)));
    }

    private Path file(long studyId, int observationId, UUID attachmentId) {
        return baseDir.resolve(String.valueOf(studyId))
                .resolve(String.valueOf(observationId))
                .resolve(attachmentId.toString());
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.warn("Unable to delete attachment file {}", file, e);
        }
    }

    private static void deleteDirQuietly(Path dir) {
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (IOException e) {
            LOG.warn("Unable to delete attachment directory {}", dir, e);
        }
    }

}
