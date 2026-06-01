CREATE TABLE IF NOT EXISTS nvpairs_goaltemplates (
    study_id BIGINT NOT NULL,
    template_id INT,
    name VARCHAR,
    value bytea NOT NULL,

    PRIMARY KEY (study_id, template_id, name),
    FOREIGN KEY (study_id, template_id) REFERENCES goal_templates(study_id, template_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS nvpairs_goals (
     study_id BIGINT NOT NULL,
     goal_id INT,
     name VARCHAR,
     value bytea NOT NULL,

     PRIMARY KEY (study_id, goal_id, name),
     FOREIGN KEY (study_id, goal_id) REFERENCES goal(study_id, goal_id) ON DELETE CASCADE
);

