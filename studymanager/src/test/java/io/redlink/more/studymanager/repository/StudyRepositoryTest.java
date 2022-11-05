package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.model.Study;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test-containers-flyway")
class StudyRepositoryTest {
    @Autowired
    private StudyRepository studyRepository;

    @BeforeEach
    void deleteAll() {
        studyRepository.clear();
    }

    @Test
    @DisplayName("Study is inserted in database and returned")
    public void testInsert() {
        Study study = new Study()
                .setTitle("some title");

        Study studyResponse = studyRepository.insert(study);

        assertThat(studyResponse.getStudyId()).isNotNull();
        assertThat(studyResponse.getTitle()).isEqualTo(study.getTitle());
        assertThat(studyResponse.getStudyState()).isEqualTo(Study.Status.DRAFT);
    }
    @Test
    @DisplayName("Study is updated in database and returned")
    public void testUpdate() throws InterruptedException {
        Study insert = new Study()
                .setTitle("some title");

        Study inserted = studyRepository.insert(insert);

        Study update = new Study()
                .setStudyId(inserted.getStudyId())
                .setTitle("some new title");

        Study updated = studyRepository.update(update);

        Study queried = studyRepository.getById(inserted.getStudyId());

        assertThat(queried.getTitle()).isEqualTo(updated.getTitle());
        assertThat(queried.getStudyId()).isEqualTo(updated.getStudyId());
        assertThat(queried.getCreated()).isEqualTo(updated.getCreated());

        assertThat(update.getTitle()).isEqualTo(updated.getTitle());
        assertThat(inserted.getStudyId()).isEqualTo(updated.getStudyId());
        assertThat(inserted.getCreated()).isEqualTo(updated.getCreated());
        assertThat(inserted.getModified().getTime()).isLessThan(updated.getModified().getTime());
    }

    @Test
    @DisplayName("Studies are deleted and listed correctly")
    public void testListAndDelete() {
        Study s1 = studyRepository.insert(new Study());
        Study s2 = studyRepository.insert(new Study());
        Study s3 = studyRepository.insert(new Study());

        assertThat(studyRepository.listStudyOrderByModifiedDesc().size()).isEqualTo(3);
        studyRepository.deleteById(s1.getStudyId());
        assertThat(studyRepository.listStudyOrderByModifiedDesc().size()).isEqualTo(2);
        studyRepository.deleteById(s2.getStudyId());
        assertThat(studyRepository.listStudyOrderByModifiedDesc().size()).isEqualTo(1);
        studyRepository.deleteById(s2.getStudyId());
        assertThat(studyRepository.listStudyOrderByModifiedDesc().size()).isEqualTo(1);
        studyRepository.deleteById(s3.getStudyId());
        assertThat(studyRepository.listStudyOrderByModifiedDesc().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Study states are set correctly")
    public void testSetState() {
        Study study = studyRepository.insert(new Study());
        assertThat(study.getStudyState()).isEqualTo(Study.Status.DRAFT);
        assertThat(study.getStartDate()).isNull();

        studyRepository.setStateById(study.getStudyId(), Study.Status.ACTIVE);
        study = studyRepository.getById(study.getStudyId());
        assertThat(study.getStudyState()).isEqualTo(Study.Status.ACTIVE);
        assertThat(study.getStartDate()).isNotNull();
        assertThat(study.getEndDate()).isNull();

        studyRepository.setStateById(study.getStudyId(), Study.Status.PAUSED);
        study = studyRepository.getById(study.getStudyId());
        assertThat(study.getStudyState()).isEqualTo(Study.Status.PAUSED);
        assertThat(study.getStartDate()).isNotNull();
        assertThat(study.getEndDate()).isNull();

        studyRepository.setStateById(study.getStudyId(), Study.Status.CLOSED);
        study = studyRepository.getById(study.getStudyId());
        assertThat(study.getStudyState()).isEqualTo(Study.Status.CLOSED);
        assertThat(study.getStartDate()).isNotNull();
        assertThat(study.getEndDate()).isNotNull();
    }

}
