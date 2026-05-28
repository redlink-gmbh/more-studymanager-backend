-- Allows to define for a goal when users are ask about goal adherence
-- for a goal that is based on this template
CREATE TABLE goal_goal_adherence_checks (
    study_id BIGINT NOT NULL,
    goal_id INT NOT NULL,
    check_id INT NOT NULL,

    PRIMARY KEY (study_id, goal_id, check_id),
    FOREIGN KEY (study_id, goal_id) REFERENCES goal(study_id, goal_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, check_id) REFERENCES goal_adherence_checks(study_id, check_id) ON DELETE RESTRICT
);

