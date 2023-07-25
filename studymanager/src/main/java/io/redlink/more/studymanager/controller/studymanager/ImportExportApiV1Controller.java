package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.GenerateDownloadToken200ResponseDTO;
import io.redlink.more.studymanager.api.v1.model.StudyDTO;
import io.redlink.more.studymanager.api.v1.model.StudyImportExportDTO;
import io.redlink.more.studymanager.api.v1.webservices.ImportExportApi;
import io.redlink.more.studymanager.controller.RequiresStudyRole;
import io.redlink.more.studymanager.model.DownloadToken;
import io.redlink.more.studymanager.model.StudyRole;
import io.redlink.more.studymanager.model.transformer.ImportExportTransformer;
import io.redlink.more.studymanager.model.transformer.StudyTransformer;
import io.redlink.more.studymanager.repository.DownloadTokenRepository;
import io.redlink.more.studymanager.service.ImportExportService;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import io.redlink.more.studymanager.utils.MapperUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;


@RestController
@RequestMapping(value = "/api/v1")
public class ImportExportApiV1Controller implements ImportExportApi {

    private final ImportExportService service;

    private final DownloadTokenRepository tokenRepository;

    private final OAuth2AuthenticationService authService;


    public ImportExportApiV1Controller(ImportExportService service, DownloadTokenRepository tokenRepository, OAuth2AuthenticationService authService) {
        this.service = service;
        this.tokenRepository = tokenRepository;
        this.authService = authService;
    }


    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    public ResponseEntity<Resource> exportParticipants(Long studyId) {
        final var currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(
                service.exportParticipants(studyId, currentUser)
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    public ResponseEntity<Void> importParticipants(Long studyId, MultipartFile file) {
        try {
            service.importParticipants(studyId, file.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(201).build();
    }


    @Override
    @RequiresStudyRole(StudyRole.STUDY_ADMIN)
    public ResponseEntity<StudyImportExportDTO> exportStudy(Long studyId) {
        final var currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(
                ImportExportTransformer
                        .toStudyImportExportDTO_V1(
                                service.exportStudy(studyId, currentUser)
                        )
        );
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    public ResponseEntity<GenerateDownloadToken200ResponseDTO> generateDownloadToken(Long studyId) {
        return ResponseEntity.ok(new GenerateDownloadToken200ResponseDTO().token(
                tokenRepository.createToken(studyId).getToken()
        ));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/studies/{studyId}/export/studydata/{token}",
            produces = { "application/json" }
    )
    public void exportStudyData(@PathVariable Long studyId, @PathVariable("token") String token, HttpServletResponse response) throws IOException {
        Optional<DownloadToken> dt = tokenRepository.getToken(token).filter(t -> t.getStudyId().equals(studyId));
        if(dt.isPresent()) {
            response.setHeader("Content-Disposition", "attachment;filename=" + dt.get().getFilename());
            service.exportStudyData(response.getOutputStream(), studyId);
        } else {
            response.setStatus(403);
        }
    }

    @Override
    public ResponseEntity<StudyDTO> importStudy(MultipartFile file) {
        try {
            final var currentUser = authService.getCurrentUser();
            StudyImportExportDTO imp = MapperUtils.MAPPER.readValue(file.getInputStream(), StudyImportExportDTO.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    StudyTransformer.toStudyDTO_V1(
                            service.importStudy(ImportExportTransformer.fromStudyImportExportDTO_V1(imp), currentUser)
                    )
            );
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
