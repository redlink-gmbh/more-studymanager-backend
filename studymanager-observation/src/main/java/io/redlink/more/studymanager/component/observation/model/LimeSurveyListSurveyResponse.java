package io.redlink.more.studymanager.component.observation.model;

import java.util.List;

public record LimeSurveyListSurveyResponse (
        List<SurveyData> result,
        int id,
        String error
) {
}
