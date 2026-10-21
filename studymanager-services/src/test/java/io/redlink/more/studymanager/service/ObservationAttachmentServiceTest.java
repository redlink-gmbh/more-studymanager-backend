/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.configuration.JPAConfiguration;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.BadStudyStateException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.Contact;
import io.redlink.more.studymanager.model.Observation;
import io.redlink.more.studymanager.model.ObservationAttachment;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.scheduler.StudyWideEvent;
import io.redlink.more.studymanager.repository.ObservationAttachmentRepository;
import io.redlink.more.studymanager.repository.ObservationRepository;
import io.redlink.more.studymanager.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {ObservationAttachmentRepository.class, ObservationRepository.class, StudyRepository.class, JPAConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles({"test", "test-containers-flyway"})
class ObservationAttachmentServiceTest {

    @Autowired
    private ObservationAttachmentRepository attachmentRepository;
    @Autowired
    private ObservationRepository observationRepository;
    @Autowired
    private StudyRepository studyRepository;

    @TempDir
    Path dir;

    @Test
    void testAddGetDeleteAndCascade() throws Exception {
        StudyStateService studyStateService = mock(StudyStateService.class);
        var service = new ObservationAttachmentService(attachmentRepository, studyStateService, dir);
        long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        int observationId = observationRepository.insert(new Observation()
                .setStudyId(studyId).setTitle("info").setType("info-observation").setHidden(false).setNoSchedule(false)
                .setProperties(new ObservationProperties(Map.of())).setSchedule(new StudyWideEvent())).getObservationId();

        ObservationAttachment a = service.addAttachment(studyId, observationId, "a.pdf", "application/pdf", 3,
                new ByteArrayInputStream("abc".getBytes()));
        assertThat(a.size()).isEqualTo(3);
        assertThat(service.getAttachment(studyId, observationId, a.attachmentId())).contains(a);
        assertThat(Files.readString(service.getFile(a))).isEqualTo("abc");
        assertThat(service.getFile(a)).startsWith(dir.toAbsolutePath());
        assertThat(service.listAttachments(studyId, observationId)).containsExactly(a);

        assertThatThrownBy(() -> service.addAttachment(studyId, observationId, "big", null,
                ObservationAttachmentService.MAX_SIZE_BYTES + 1, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.addAttachment(studyId, observationId + 1, "x", null, 1,
                new ByteArrayInputStream(new byte[1])))
                .isInstanceOf(NotFoundException.class);

        when(studyStateService.assertStudyState(studyId, Study.Status.CONTENT_DELETION_STATES)).thenThrow(BadStudyStateException.state());
        assertThatThrownBy(() -> service.deleteAttachment(studyId, observationId, a.attachmentId()))
                .isInstanceOf(BadStudyStateException.class);
        assertThat(service.getAttachment(studyId, observationId, a.attachmentId())).contains(a);
        reset(studyStateService);

        service.deleteAttachment(studyId, observationId, a.attachmentId());
        assertThat(service.getAttachment(studyId, observationId, a.attachmentId())).isEmpty();
        assertThat(service.getFile(a)).doesNotExist();

        // attachments of removed messages are deleted, unreferenced uploads are kept
        ObservationAttachment kept = service.addAttachment(studyId, observationId, "k", null, 1, new ByteArrayInputStream(new byte[1]));
        ObservationAttachment dropped = service.addAttachment(studyId, observationId, "d", null, 1, new ByteArrayInputStream(new byte[1]));
        ObservationAttachment unsaved = service.addAttachment(studyId, observationId, "u", null, 1, new ByteArrayInputStream(new byte[1]));
        service.deleteDropped(studyId, observationId,
                Map.of("messages", List.of(
                        Map.of("id", "1", "attachments", List.of(kept.attachmentId().toString())),
                        Map.of("id", "2", "attachments", List.of(dropped.attachmentId().toString())))),
                Map.of("messages", List.of(
                        Map.of("id", "1", "attachments", List.of(kept.attachmentId().toString())))));
        assertThat(service.listAttachments(studyId, observationId)).containsExactlyInAnyOrder(kept, unsaved);
        assertThat(service.getFile(dropped)).doesNotExist();

        ObservationAttachment b = service.addAttachment(studyId, observationId, "b.txt", "text/plain", 1,
                new ByteArrayInputStream("b".getBytes()));
        observationRepository.deleteObservation(studyId, observationId);
        service.deleteFiles(studyId, observationId);
        assertThat(service.listAttachments(studyId, observationId)).isEmpty();
        assertThat(service.getFile(b)).doesNotExist();
    }
}
