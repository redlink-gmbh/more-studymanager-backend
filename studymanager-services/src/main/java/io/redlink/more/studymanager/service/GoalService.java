package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.GoalTemplateFactory;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.GoalAdherenceCheck;
import io.redlink.more.studymanager.model.GoalTemplate;
import io.redlink.more.studymanager.model.GoalTopic;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.StudyGoalConfig;
import io.redlink.more.studymanager.repository.goals.GoalConfigurationRepository;
import io.redlink.more.studymanager.repository.goals.GoalRepository;
import io.redlink.more.studymanager.repository.goals.GoalTemplateRepository;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final StudyStateService studyStateService;

    private final GoalConfigurationRepository goalConfigRepo;
    private final GoalTemplateRepository goalTemplateRepo;
    private final GoalRepository goalRepo;
    private final ApplicationContext applicationContext;

    public GoalService(
            StudyStateService studyStateService,
            GoalConfigurationRepository goalConfigRepo,
            GoalTemplateRepository goalTemplateRepo,
            GoalRepository goalRepo,
            ApplicationContext applicationContext) {
        this.studyStateService = studyStateService;
        this.goalConfigRepo = goalConfigRepo;
        this.goalTemplateRepo = goalTemplateRepo;
        this.goalRepo = goalRepo;
        this.applicationContext = applicationContext;
    }

    public StudyGoalConfig getGoalConfig(long studyId){
        return goalConfigRepo.getStudyGoalConfig(studyId);
    }

    @Transactional
    public StudyGoalConfig setGoalConfig(StudyGoalConfig studyGoalConfig) {
        studyStateService.assertStudyNotInState(studyGoalConfig.getStudyId(), Study.Status.CLOSED);
        return goalConfigRepo.saveStudyGoalConfig(studyGoalConfig);
    }


    public Collection<GoalTopic> getGoalTopics(long studyId) {
        return goalConfigRepo.listTopics(studyId);
    }

    public Collection<GoalAdherenceCheck> getGoalAdherenceChecks(long studyId) {
        return goalConfigRepo.listChecks(studyId);
    }

    /**
     * Sets the adherence checks for the study to the parsed list
     * @param studyId the study id
     * @param goalAdherenceChecks the adherence chekcs. NOTE checks with a different studyId will be ignored!
     * @return the updated adherence checks
     */
    @Transactional
    public Collection<GoalAdherenceCheck> setGoalAdherenceChecks(Long studyId, List<GoalAdherenceCheck> goalAdherenceChecks) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(studyId), Study.Status.CLOSED);
        goalConfigRepo.deleteChecks(studyId);
        return upsertAdherenceChecks(studyId, goalAdherenceChecks);
    }

    /**
     * Upserts the parsed adherence checks for the parsed study. Adherence checks for other studies are ignored
     * @param studyId the studyId
     * @param checks the chekcs to insert or update
     * @return the inserted and updated checks
     */
    @Transactional
    public Collection<GoalAdherenceCheck> upsertAdherenceChecks(Long studyId, List<GoalAdherenceCheck> checks) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(studyId), Study.Status.CLOSED);
        return checks.stream()
                .filter(goalAdherenceCheck -> studyId.equals(goalAdherenceCheck.getStudyId()))
                .map(goalConfigRepo::upsertCheck)
                .sorted(Comparator.comparing(GoalAdherenceCheck::getStudyId).thenComparing(GoalAdherenceCheck::getCheckId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public void deleteAdherenceChecks(long studyId) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(studyId), Study.Status.CLOSED);
        goalConfigRepo.deleteChecks(studyId);
    }


    public List<GoalTopic> listGoalTopics(long studyId) {
        return goalConfigRepo.listTopics(studyId);
    }

    @Transactional
    public GoalTopic setGoalTopic(GoalTopic goalTopic) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(goalTopic.getStudyId()), Study.Status.CLOSED);
        return goalConfigRepo.saveTopic(goalTopic);
    }

    @Transactional
    public void deleteGoalTopic(Long studyId, String key) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(studyId), Study.Status.CLOSED);
        goalConfigRepo.deleteTopic(Objects.requireNonNull(studyId), Objects.requireNonNull(key));
    }

    public GoalTopic getGoalTopic(Long studyId, String key) {
        return goalConfigRepo.getTopic(Objects.requireNonNull(studyId), Objects.requireNonNull(key));
    }

    public List<GoalTemplate> listGoalTemplates(long studyId) {
        return goalTemplateRepo.listGoalTemplates(studyId);
    }

    public GoalTemplate addGoalTemplate(GoalTemplate goalTemplate) {
        studyStateService.assertStudyNotInState(Objects.requireNonNull(goalTemplate.getStudyId()), Study.Status.CLOSED);
        return goalTemplateRepo.insert(validate(goalTemplate));
    }

    public GoalTemplate updateGoalTemplate(GoalTemplate goalTemplate) {
        studyStateService.assertStudyNotInState(goalTemplate.getStudyId(), Study.Status.CLOSED);
        return goalTemplateRepo.update(validate(goalTemplate));
    }

    public GoalTemplate importGoalTemplate(Long studyId, GoalTemplate goalTemplate) {
        final GoalTemplateFactory factory = factory(goalTemplate);
        if (factory == null) {
            throw NotFoundException.ObservationFactory(goalTemplate.getType());
        }
        GoalTemplateProperties props = (GoalTemplateProperties) factory.preImport(goalTemplate.getProperties());
        goalTemplate.setProperties(props);
        return goalTemplateRepo.doImport(studyId, goalTemplate);
    }

    public void deleteGoalTemplate(Long studyId, Integer goalTemplateId) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        goalTemplateRepo.deleteGoalTemplate(studyId, goalTemplateId);
    }

    public Optional<GoalTemplate> getGoalTemplate(Long studyId, Integer goalTemplateId) {
        try {
            return Optional.ofNullable(goalTemplateRepo.getById(studyId, goalTemplateId));
        } catch (BadRequestException e) {
            return Optional.empty();
        }
    }


    /**
     * Ensures the goalTemplageFactory for the parsed goalTemplate
     * @param template the goal template
     * @return the goal template factory
     * @throws NotFoundException if the {@link GoalTemplateFactory} for the parsed observation is not present
     */
    private GoalTemplateFactory factory(GoalTemplate template) {
        return getGoalTemplateFactory(template)
                .orElseThrow(() -> new NotFoundException(String.format("GoalTemplateFactory for GoalTemplate[study: %s, id:%s, type: %s]",
                        template.getStudyId(), template.getTemplateId(), template.getType())));
    }

    private GoalTemplate validate(GoalTemplate goalTemplate) {
        try {
            factory(goalTemplate).validate(goalTemplate.getProperties());
        } catch (ConfigurationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
        return goalTemplate;
    }

    public Optional<GoalTemplateFactory> getGoalTemplateFactory(GoalTemplate template) {
        try {
            return Optional.of(applicationContext.getBean(template.getType(), GoalTemplateFactory.class));
        } catch (NoSuchBeanDefinitionException | BeanNotOfRequiredTypeException e){
            return Optional.empty();
        }
    }


}
