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
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.Contact;
import io.redlink.more.studymanager.model.Observation;
import io.redlink.more.studymanager.model.ObservationAttachment;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.scheduler.StudyWideEvent;
import io.redlink.more.studymanager.repository.ObservationRepository;
import io.redlink.more.studymanager.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {ObservationRepository.class, StudyRepository.class, JPAConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles({"test", "test-containers-flyway"})
class ObservationAttachmentServiceTest {

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate;
    @Autowired
    private ObservationRepository observationRepository;
    @Autowired
    private StudyRepository studyRepository;

    @TempDir
    Path dir;

    @Test
    void testAddGetDeleteAndCascade() throws Exception {
        var service = new ObservationAttachmentService(namedTemplate, mock(StudyStateService.class), dir);
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

        service.deleteAttachment(studyId, observationId, a.attachmentId());
        assertThat(service.getAttachment(studyId, observationId, a.attachmentId())).isEmpty();
        assertThat(service.getFile(a)).doesNotExist();

        ObservationAttachment b = service.addAttachment(studyId, observationId, "b.txt", "text/plain", 1,
                new ByteArrayInputStream("b".getBytes()));
        observationRepository.deleteObservation(studyId, observationId);
        service.deleteFiles(studyId, observationId);
        assertThat(service.listAttachments(studyId, observationId)).isEmpty();
        assertThat(service.getFile(b)).doesNotExist();
    }
}
