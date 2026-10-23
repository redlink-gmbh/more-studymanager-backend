/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.EndpointTokenDTO;
import io.redlink.more.studymanager.api.v1.model.ObservationAttachmentDTO;
import io.redlink.more.studymanager.api.v1.model.ObservationDTO;
import io.redlink.more.studymanager.api.v1.webservices.ObservationsApi;
import io.redlink.more.studymanager.audit.Audited;
import io.redlink.more.studymanager.controller.RequiresStudyRole;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.model.EndpointToken;
import io.redlink.more.studymanager.model.Observation;
import io.redlink.more.studymanager.model.ObservationAttachment;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.StudyRole;
import io.redlink.more.studymanager.model.transformer.EndpointTokenTransformer;
import io.redlink.more.studymanager.model.transformer.ObservationTransformer;
import io.redlink.more.studymanager.service.IntegrationService;
import io.redlink.more.studymanager.service.ObservationAttachmentService;
import io.redlink.more.studymanager.service.ObservationService;
import io.redlink.more.studymanager.service.StudyStateService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ObservationsApiV1Controller implements ObservationsApi {

    private final ObservationService service;

    private final IntegrationService integrationService;
    private final StudyStateService studyStateService;
    private final ObservationAttachmentService attachmentService;

    public ObservationsApiV1Controller(ObservationService service, IntegrationService integrationService,
                                       StudyStateService studyStateService, ObservationAttachmentService attachmentService) {
        this.service = service;
        this.integrationService = integrationService;
        this.studyStateService = studyStateService;
        this.attachmentService = attachmentService;
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ObservationDTO> addObservation(Long studyId, ObservationDTO observationDTO) {
        Observation observation = service.addObservation(
                ObservationTransformer.fromObservationDTO_V1(observationDTO.studyId(studyId))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ObservationTransformer.toObservationDTO_V1(observation)
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<Void> deleteObservation(Long studyId, Integer observationId) {
        service.deleteObservation(studyId, observationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<List<ObservationDTO>> listObservations(Long studyId) {
        return ResponseEntity.ok().body(
                service.listObservations(studyId).stream()
                        .map(ObservationTransformer::toObservationDTO_V1)
                        .toList()
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ObservationDTO> updateObservation(Long studyId, Integer observationId, ObservationDTO observationDTO) {
        Observation observation = service.updateObservation(
                ObservationTransformer.fromObservationDTO_V1(observationDTO.studyId(studyId).observationId(observationId))
        );
        return ResponseEntity.status(HttpStatus.OK).body(
                ObservationTransformer.toObservationDTO_V1(observation)
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<EndpointTokenDTO> createToken(Long studyId, Integer observationId, EndpointTokenDTO token) {
        if (token.getTokenLabel().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<EndpointToken> addedToken = integrationService.addToken(studyId, observationId, token.getTokenLabel());
        if (addedToken.isEmpty()) {
            throw new BadRequestException("Token with given label already exists for given observation");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                EndpointTokenTransformer.toEndpointTokenDTO(
                        addedToken.get()
                )
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<EndpointTokenDTO> updateTokenLabel(Long studyId, Integer observationId, Integer tokenId, EndpointTokenDTO endpointToken) {
        Optional<EndpointToken> token = integrationService.updateToken(studyId, observationId, tokenId, endpointToken.getTokenLabel());

        return ResponseEntity.of(
                token.map(EndpointTokenTransformer::toEndpointTokenDTO)
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<EndpointTokenDTO> getToken(Long studyId, Integer observationId, Integer tokenId) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        Optional<EndpointToken> token = integrationService.getToken(studyId, observationId, tokenId);
        if (token.isEmpty()) {
            throw new BadRequestException("Token with given id doesn't exist for given observation");
        }
        return ResponseEntity.ok().body(
                EndpointTokenTransformer.toEndpointTokenDTO(
                        token.get()
                )
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<List<EndpointTokenDTO>> getTokens(Long studyId, Integer observationId) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        return ResponseEntity.ok().body(
                EndpointTokenTransformer.toEndpointTokensDTO(
                        integrationService.getTokens(studyId, observationId)
                )
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<Void> deleteToken(Long studyId, Integer observationId, Integer tokenId) {
        integrationService.deleteToken(studyId, observationId, tokenId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ObservationAttachmentDTO> addObservationAttachment(Long studyId, Integer observationId, MultipartFile file) {
        try (var content = file.getInputStream()) {
            ObservationAttachment attachment = attachmentService.addAttachment(studyId, observationId,
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
            return ResponseEntity.status(HttpStatus.CREATED).body(toAttachmentDTO(attachment));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    public ResponseEntity<List<ObservationAttachmentDTO>> listObservationAttachments(Long studyId, Integer observationId) {
        return ResponseEntity.ok(attachmentService.listAttachments(studyId, observationId).stream()
                .map(ObservationsApiV1Controller::toAttachmentDTO)
                .toList());
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    public ResponseEntity<Resource> getObservationAttachment(Long studyId, Integer observationId, UUID attachmentId) {
        return attachmentService.getAttachment(studyId, observationId, attachmentId)
                .map(a -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment().filename(a.filename(), StandardCharsets.UTF_8).build().toString())
                        .header("X-Content-Type-Options", "nosniff")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(a.size())
                        .body((Resource) new FileSystemResource(attachmentService.getFile(a))))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<Void> deleteObservationAttachment(Long studyId, Integer observationId, UUID attachmentId) {
        attachmentService.deleteAttachment(studyId, observationId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private static ObservationAttachmentDTO toAttachmentDTO(ObservationAttachment a) {
        return new ObservationAttachmentDTO()
                .attachmentId(a.attachmentId())
                .filename(a.filename())
                .contentType(a.contentType())
                .size(a.size())
                .created(a.created());
    }
}
