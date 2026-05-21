package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.GoalTemplateDTO;
import io.redlink.more.studymanager.api.v1.model.GoalTopicDTO;
import io.redlink.more.studymanager.api.v1.model.StudyGoalConfigDTO;
import io.redlink.more.studymanager.api.v1.model.StudyGoalConfigDataDTO;
import io.redlink.more.studymanager.api.v1.webservices.GoalsApi;
import io.redlink.more.studymanager.audit.Audited;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.AuthenticatedUser;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.StudyGoalConfig;
import io.redlink.more.studymanager.model.transformer.GoalV1Transformer;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import io.redlink.more.studymanager.service.StudyService;
import io.redlink.more.studymanager.service.GoalService;
import io.redlink.more.studymanager.utils.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class GoalsApiV1Controller implements GoalsApi {


    private final StudyService studyService;
    private final GoalService goalService;
    private final OAuth2AuthenticationService authService;

    public GoalsApiV1Controller(
            StudyService studyService,
            GoalService goalService,
            OAuth2AuthenticationService authService) {
        this.studyService = studyService;
        this.goalService = goalService;
        this.authService = authService;
    }

    /* ---
     * Goal Study Config API
     * ---
     */

    @Override
    @Audited
    public ResponseEntity<StudyGoalConfigDataDTO> getGoalConfig(Long studyId) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        var config = goalService.getGoalConfig(studyId);
        if(config == null) {
            //no custom goal config set fpr this stury - use the default one (for now empty)
            config = new StudyGoalConfig();
        }
        return ResponseEntity.ok(GoalV1Transformer.toStudyGoalConfigDataDTO_V1(
                config,
                goalService.getGoalTopics(studyId),
                goalService.getGoalAdherenceChecks(studyId)));
    }

    @Override
    @Audited
    public ResponseEntity<StudyGoalConfigDataDTO> setGoalConfig(Long studyId, StudyGoalConfigDTO studyGoalConfigDTO) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        var config = GoalV1Transformer.toStudyGoalConfig(studyGoalConfigDTO, studyId);
        var checks = GoalV1Transformer.toGoalAdherenceChecks(studyGoalConfigDTO, studyId);
        //process the adherence checks first as this might result in a CONFLICT if one tries to delete a used one
        var updatedChecks = goalService.setGoalAdherenceChecks(studyId, checks);
        var updatedConfig = goalService.setGoalConfig(config);
        if(updatedConfig == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(GoalV1Transformer.toStudyGoalConfigDataDTO_V1(
                    updatedConfig,
                    goalService.getGoalTopics(studyId),
                    updatedChecks));
        }
    }

    /* ---
     * Goal Topic API
     * ---
     */

    @Override
    @Audited
    public ResponseEntity<GoalTopicDTO> createGoalTopic(Long studyId, GoalTopicDTO goalTopicDTO) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        if(goalTopicDTO.getKey() == null) {
            goalTopicDTO.setKey(SlugUtils.toSlug(goalTopicDTO.getTitle()));
        }
        if(!SlugUtils.isSlug(goalTopicDTO.getKey())) {
            throw new BadRequestException(String.format("The key of the parsed Topic is not a valid Slug (key: %s, suggested: %s)",
                    goalTopicDTO.getKey(), SlugUtils.toSlug(goalTopicDTO.getKey())));
        }
        if(goalService.getGoalTopic(studyId, goalTopicDTO.getKey()) != null){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return responseGoalTopic(studyId, goalTopicDTO);
    }

    @Override
    @Audited
    public ResponseEntity<GoalTopicDTO> updateGoalTopic(Long studyId, String key, GoalTopicDTO goalTopicDTO) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        return responseGoalTopic(studyId, goalTopicDTO);
    }

    @Override
    @Audited
    public ResponseEntity<Void> deleteGoalTopic(Long studyId, String key) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        goalService.deleteGoalTopic(studyId, key);
        return ResponseEntity.noContent().build();
    }

    /* ---
     * Goal Template API
     * ---
     */

    @Override
    @Audited
    public ResponseEntity<List<GoalTemplateDTO>> listGoalTemplates(Long studyId) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        return ResponseEntity.ok(goalService.listGoalTemplates(studyId).stream()
                .map(GoalV1Transformer::toGoalTemplateDTO_V1)
                .collect(Collectors.toList()));
    }

    @Override
    @Audited
    public ResponseEntity<GoalTemplateDTO> addGoalTemplate(Long studyId, GoalTemplateDTO goalTemplateDTO) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        return ResponseEntity.ok(
                GoalV1Transformer.toGoalTemplateDTO_V1(
                        goalService.addGoalTemplate(GoalV1Transformer.toGoalTemplate(goalTemplateDTO, studyId))));
    }

    @Override
    @Audited
    public ResponseEntity<GoalTemplateDTO> updateGoalTemplate(Long studyId, Integer templateId, GoalTemplateDTO goalTemplateDTO) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        return ResponseEntity.ok(
                GoalV1Transformer.toGoalTemplateDTO_V1(
                        goalService.updateGoalTemplate(
                                GoalV1Transformer.toGoalTemplate(goalTemplateDTO, studyId, templateId))));
    }

    @Override
    @Audited
    public ResponseEntity<Void> deleteGoalTemplate(Long studyId, Integer templateId) {
        final var currentUser = authService.getCurrentUser();
        validateStudyForUser(studyId, currentUser);
        goalService.deleteGoalTemplate(studyId, templateId);
        return ResponseEntity.noContent().build();
    }

    /*
     * Utility methods
     */
    private ResponseEntity<GoalTopicDTO> responseGoalTopic(Long studyId, GoalTopicDTO goalTopicDTO) {
        var topic = goalService.setGoalTopic(GoalV1Transformer.toGoalTopic(goalTopicDTO, studyId));
        if(topic == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(GoalV1Transformer.toGoalTopicDTO_V1(topic));
    }

    private void validateStudyForUser(Long studyId, AuthenticatedUser currentUser) {
        Study study = studyService.getStudy(studyId, currentUser)
                .orElseThrow(() -> new NotFoundException(String.format("Study %s not found", studyId)));
    }


}
