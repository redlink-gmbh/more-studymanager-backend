package io.redlink.more.studymanager.component.goaltemplate;

import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class EatAmountOfGoalTemplateFactoryTest {

    private EatAmountOfGoalTemplateFactory factory;

    @BeforeEach
    public void init() {
        factory = new EatAmountOfGoalTemplateFactory();
    }

    @AfterEach
    public void tearDown() {
        factory = null;
    }

    @Test
    public void testPropertyValidation(){
        Map<String,Object> properties = new HashMap<>();
        properties.put("app-title", "title");
        properties.put("app-description", "description");
        properties.put("days-of-week", Map.of("lower", 7, "upper", 7));
        properties.put("goal-preview", "Ich esse mindestens <goal.amount> Portionen <goal.unit>[ an <days-of-week> Tagen der Woche].");
        properties.put("goal-title-state", true);
        properties.put("goal.amount", Map.of("lower", 1, "upper", 1));
        properties.put("goal.unit", "Portionen Obst");
        properties.put("self-report-question", "Wie viele Portionen hast Du heute gegessen? Bitte trage den Wert ein.");
        properties.put("self-report-time", "Abends");
        properties.put("status-75-reached", "Dein Ziel ist zum greifen nah. Ein bisschen mehr und Du hast es geschafft!");
        properties.put("status-100-reached", "Du hast dein Tagesziel erfolgreich gemeistert. Weiter so!");
        properties.put("status-not-reached", "Du bist auf den richtigen Weg. Jede Portion zählt für Dein wohlbefinden.");

        //Validate that this works without exception
        factory.validate(new GoalTemplateProperties(properties));

    }
}
