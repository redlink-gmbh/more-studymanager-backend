/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.Resources;
import io.redlink.more.studymanager.configuration.ElasticConfiguration;
import io.redlink.more.studymanager.core.io.RuntimeFieldKind;
import io.redlink.more.studymanager.core.io.RuntimeFieldSpec;
import io.redlink.more.studymanager.core.io.Timeframe;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.data.ElasticActionDataPoint;
import io.redlink.more.studymanager.model.data.ElasticObservationDataPoint;
import io.redlink.more.studymanager.utils.MapperUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = ElasticSearchServiceTest.EnvInitializer.class,
        classes = {
                ElasticService.class,
                ElasticConfiguration.class,
                JacksonAutoConfiguration.class
        })
class ElasticSearchServiceTest {

    private final static Logger LOG = LoggerFactory.getLogger(ElasticSearchServiceTest.class);

    @Container
    public static ElasticsearchContainer elasticContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.4")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("action.auto_create_index", "true")
            .withEnv("bootstrap.memory_lock", "true")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private ElasticsearchClient client;

    @Test
    void testParticipantsThatMapQuery() throws InterruptedException {
        Study study = new Study().setStudyId(28L);

        indexDoc(study, 1);
        indexDoc(study, 2);

        Thread.sleep(1000);

        List<Integer> ids = elasticService.participantsThatMapQuery(
                study.getStudyId(), null, "data_z:[* TO *]", List.of(),
                new Timeframe(Instant.parse("2022-10-24T10:00:00Z"), Instant.parse("2022-10-24T15:00:00Z"))
        );
        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isEqualTo(10);

        elasticService.deleteIndex(study);
    }

    /**
     * LimeSurvey stores every answer as string.
     */
    private void indexLimeSurveyAnswers(Study study) {
        indexData(study, 1, Map.of("EPDSsumt1", "23", "submitted", "true"));
        indexData(study, 2, Map.of("EPDSsumt1", "23.0000000000"));
        indexData(study, 3, Map.of("EPDSsumt1", "5"));
        indexData(study, 4, Map.of("EPDSsumt1", ""));
        indexData(study, 5, Map.of("submitted", "false"));
        indexData(study, 6, Map.of("EPDSsumt1", "abc"));
        indexData(study, 7, Map.of("PRAQt1[PRAQ1t1]", "14"));
        indexData(study, 8, Map.of("EPDSsumt1", "-3"));
        indexData(study, 9, Map.of("EPDSsumt1", "5.5"));
    }

    @Test
    void testParticipantsThatMapQueryWithLongRuntimeFieldOnStrings() {
        Study study = new Study().setStudyId(40L);
        indexLimeSurveyAnswers(study);

        List<RuntimeFieldSpec> rf = List.of(
                new RuntimeFieldSpec("long_data_EPDSsumt1", "data_EPDSsumt1", RuntimeFieldKind.LONG),
                new RuntimeFieldSpec("long_data_PRAQt1[PRAQ1t1]", "data_PRAQt1[PRAQ1t1]", RuntimeFieldKind.LONG));

        assertThat(query(study, "long_data_EPDSsumt1:[\"13\" TO *]", rf)).containsExactlyInAnyOrder(1, 2);
        // alphabetically "23" < "3", numerically it must match
        assertThat(query(study, "long_data_EPDSsumt1:[\"3\" TO *]", rf)).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(query(study, "long_data_EPDSsumt1:\"23\"", rf)).containsExactlyInAnyOrder(1, 2);
        // "5.5" is no whole number, it must not be truncated to 5
        assertThat(query(study, "long_data_EPDSsumt1:\"5\"", rf)).containsExactlyInAnyOrder(3);
        assertThat(query(study, "long_data_EPDSsumt1:{* TO \"0\"}", rf)).containsExactlyInAnyOrder(8);
        // empty, missing, unparsable and non-whole values must not match "!="
        assertThat(query(study, "(_exists_:long_data_EPDSsumt1 AND NOT long_data_EPDSsumt1:\"23\")", rf))
                .containsExactlyInAnyOrder(3, 8);
        assertThat(query(study, "long_data_PRAQt1\\[PRAQ1t1\\]:[\"13\" TO *]", rf)).containsExactlyInAnyOrder(7);
        // string comparison stays as it is
        assertThat(query(study, "data_submitted:true", rf)).containsExactlyInAnyOrder(1);

        elasticService.deleteIndex(study);
    }

    @Test
    void testParticipantsThatMapQueryWithDoubleRuntimeFieldOnStrings() {
        Study study = new Study().setStudyId(41L);
        indexLimeSurveyAnswers(study);

        List<RuntimeFieldSpec> rf = List.of(
                new RuntimeFieldSpec("double_data_EPDSsumt1", "data_EPDSsumt1", RuntimeFieldKind.DOUBLE));

        assertThat(query(study, "double_data_EPDSsumt1:[\"13.0\" TO *]", rf)).containsExactlyInAnyOrder(1, 2);
        assertThat(query(study, "double_data_EPDSsumt1:[\"3.0\" TO *]", rf)).containsExactlyInAnyOrder(1, 2, 3, 9);
        assertThat(query(study, "double_data_EPDSsumt1:{\"5.0\" TO \"6.0\"}", rf)).containsExactlyInAnyOrder(9);
        assertThat(query(study, "double_data_EPDSsumt1:{* TO \"0.0\"}", rf)).containsExactlyInAnyOrder(8);

        elasticService.deleteIndex(study);
    }

    @Test
    void testParticipantsThatMapQueryWithRuntimeFieldsOnNumbers() {
        Study study = new Study().setStudyId(42L);
        // the first value maps the field as long, the doc value of 5.5 is then 5
        indexData(study, 1, Map.of("score", 20));
        indexData(study, 2, Map.of("score", 5.5));
        indexData(study, 3, Map.of("score", 7));

        List<RuntimeFieldSpec> rf = List.of(
                new RuntimeFieldSpec("double_data_score", "data_score", RuntimeFieldKind.DOUBLE),
                new RuntimeFieldSpec("long_data_score", "data_score", RuntimeFieldKind.LONG));

        assertThat(query(study, "double_data_score:[\"5.3\" TO *]", rf)).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(query(study, "double_data_score:{* TO \"6.0\"}", rf)).containsExactlyInAnyOrder(2);
        assertThat(query(study, "long_data_score:\"20\"", rf)).containsExactlyInAnyOrder(1);
        assertThat(query(study, "long_data_score:[\"7\" TO *]", rf)).containsExactlyInAnyOrder(1, 3);
        // 5.5 is no whole number, it must not be truncated to 5
        assertThat(query(study, "long_data_score:\"5\"", rf)).isEmpty();
        assertThat(query(study, "(_exists_:long_data_score AND NOT long_data_score:\"20\")", rf))
                .containsExactlyInAnyOrder(3);

        elasticService.deleteIndex(study);
    }

    @Test
    void testParticipantsThatMapQueryWithBooleanRuntimeField() {
        Study study = new Study().setStudyId(43L);
        indexData(study, 1, Map.of("flag", "true"));
        indexData(study, 2, Map.of("flag", "TRUE"));
        indexData(study, 3, Map.of("flag", "yes"));
        indexData(study, 4, Map.of("flag", "1"));
        indexData(study, 5, Map.of("flag", " on "));
        indexData(study, 6, Map.of("flag", "false"));
        indexData(study, 7, Map.of("flag", "No"));
        indexData(study, 8, Map.of("flag", "0"));
        indexData(study, 9, Map.of("flag", ""));
        indexData(study, 10, Map.of("flag", "abc"));
        indexData(study, 11, Map.of("consent", true));
        indexData(study, 12, Map.of("consent", false));
        indexData(study, 13, Map.of("count", 2));
        indexData(study, 14, Map.of("count", 0));

        List<RuntimeFieldSpec> rf = List.of(
                new RuntimeFieldSpec("boolean_data_flag", "data_flag", RuntimeFieldKind.BOOLEAN),
                new RuntimeFieldSpec("boolean_data_consent", "data_consent", RuntimeFieldKind.BOOLEAN),
                new RuntimeFieldSpec("boolean_data_count", "data_count", RuntimeFieldKind.BOOLEAN));

        assertThat(query(study, "boolean_data_flag:\"true\"", rf)).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(query(study, "boolean_data_flag:\"false\"", rf)).containsExactlyInAnyOrder(6, 7, 8);
        // empty and unknown values must not match "!="
        assertThat(query(study, "(_exists_:boolean_data_flag AND NOT boolean_data_flag:\"true\")", rf))
                .containsExactlyInAnyOrder(6, 7, 8);
        assertThat(query(study, "boolean_data_consent:\"true\"", rf)).containsExactlyInAnyOrder(11);
        assertThat(query(study, "boolean_data_consent:\"false\"", rf)).containsExactlyInAnyOrder(12);
        assertThat(query(study, "boolean_data_count:\"true\"", rf)).containsExactlyInAnyOrder(13);
        assertThat(query(study, "boolean_data_count:\"false\"", rf)).containsExactlyInAnyOrder(14);

        elasticService.deleteIndex(study);
    }

    private List<Integer> query(Study study, String condition, List<RuntimeFieldSpec> runtimeFields) {
        return elasticService.participantsThatMapQuery(study.getStudyId(), null,
                "(observation_id.keyword:17 AND " + condition + ")", runtimeFields, null);
    }

    private void indexData(Study study, int participantId, Map<String, Object> data) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("participant_id", "participant_" + participantId);
        doc.put("study_id", ElasticService.getStudyIdString(study));
        doc.put("observation_id", "17");
        doc.put("observation_type", "lime-survey-observation");
        data.forEach((k, v) -> doc.put("data_" + k, v));
        try {
            client.index(i -> i
                    .index(ElasticService.getStudyIdString(study))
                    .id(String.valueOf(participantId))
                    .refresh(Refresh.True)
                    .document(doc));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRecordAction() {
        assertThatNoException().isThrownBy(() ->
                elasticService.setDataPoint(25L, new ElasticActionDataPoint(
                        UUID.randomUUID().toString(),
                        "participant_1",
                        "study_25",
                        "study_group_1",
                        "action_1",
                        "test",
                        "test",
                        Instant.now(),
                        Instant.now(),
                        Map.of()
                ))
        );
    }

    @Test
    void testExportData() throws JsonProcessingException, InterruptedException {
        for (int i = 0; i < 1200; i++) {
            setDataPoint(1L, "2", "1", "1", i);
        }
        //wait for auto commit
        Thread.sleep(2000);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (outputStream) {
            outputStream.write("[".getBytes(StandardCharsets.UTF_8));
            elasticService.exportData(outputStream, 1L, List.of(2), List.of(1), List.of(1), null, null);
            outputStream.write("]".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            //do nothing than close
        }
        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(result).isNotBlank();
        assertThat(MapperUtils.MAPPER.readValue(result, List.class)).hasSize(1200);
    }

    private void setDataPoint(Long studyId, String studyGroupId, String participantId, String observationId, int i) {
        elasticService.setDataPoint(studyId, new ElasticObservationDataPoint(
                "DP_" + studyId + "_" + participantId + "_" + i,
                "participant_" + participantId,
                "study_" + studyId,
                "study_group_" + studyGroupId,
                observationId,
                "acc-mobile-observation",
                "acc-mobile-observation",
                Instant.now(),
                Instant.now(),
                Map.of()
        ));
    }

    @Test
    void testGetParticipationData() throws InterruptedException {
        Study study = new Study().setStudyId(30L);
        indexDoc(study, 3);
        indexDoc(study, 4);
        indexDoc(study, 5);
        indexDoc(study, 6);
        indexDoc(study, 7);
        indexDoc(study, 8);

        Thread.sleep(1000);

        assertThat(elasticService.getParticipationData(30L).size()).isEqualTo(5);
        elasticService.deleteIndex(study);
    }

    private void indexDoc(Study study, int i) {
        IndexRequest<JsonData> request = IndexRequest.of(d -> d
                .index(ElasticService.getStudyIdString(study))
                .id(String.valueOf(i))
                .withJson(file(i)));
        try {
            client.index(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream file(int i) {
        try {
            return Resources.getResource("elastic/doc" + i + ".json").openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class EnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            LOG.info("TestContainer elastic.port={}", elasticContainer.getFirstMappedPort());
            TestPropertyValues.of(
                    String.format("elastic.port=%d", elasticContainer.getFirstMappedPort()),
                    "elastic.host=localhost"
            ).applyTo(applicationContext);
        }
    }
}
