-- GoalTemplates - configured by researchers (similar to observations and interventions)
CREATE TABLE goal_templates (
    study_id BIGINT NOT NULL,
    template_id INT NOT NULL,
    title VARCHAR,
    participant_title VARCHAR,
    participant_info TEXT,
    type VARCHAR NOT NULL,
    kind VARCHAR,
    study_group_id INT,
    properties JSONB,
    created TIMESTAMP NOT NULL DEFAULT now(),
    modified TIMESTAMP NOT NULL DEFAULT now(),

    PRIMARY KEY (study_id, template_id),
    FOREIGN KEY (study_id) REFERENCES studies(study_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, study_group_id) REFERENCES study_groups(study_id, study_group_id) ON DELETE SET NULL (study_group_id)
);

-- goal templates can be assigned to observation groups
-- e.g. to define that goals related to smoking only apply to
-- participants in the observation group smoking
CREATE TABLE goal_template_observation_groups (
    study_id BIGINT NOT NULL,
    template_id INT NOT NULL,
    observation_group_id INT NOT NULL,

    PRIMARY KEY (study_id, template_id, observation_group_id),
    FOREIGN KEY (study_id) REFERENCES studies(study_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, observation_group_id) REFERENCES observation_groups(study_id, observation_group_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, template_id) REFERENCES goal_templates(study_id, template_id) ON DELETE CASCADE
);

CREATE INDEX goal_templates_study_id ON goal_templates(study_id);
CREATE INDEX goal_templates_group_id ON goal_templates(study_id, study_group_id);

CREATE TABLE goal_topics (
    study_id BIGINT NOT NULL,
    key VARCHAR NOT NULL,
    title VARCHAR NOT NULL,
    description TEXT,
    created TIMESTAMP NOT NULL DEFAULT now(),
    modified TIMESTAMP NOT NULL DEFAULT now(),

    PRIMARY KEY (study_id, key),
    FOREIGN KEY (study_id) REFERENCES studies(study_id) ON DELETE CASCADE
);

CREATE TABLE goal_template_topics (
    study_id BIGINT NOT NULL,
    template_id INT NOT NULL,
    key VARCHAR NOT NULL,

    PRIMARY KEY (study_id, template_id, key),
    FOREIGN KEY (study_id, template_id) REFERENCES goal_templates(study_id, template_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, key) REFERENCES goal_topics(study_id, key) ON DELETE CASCADE
);

-- Defines title/time mappings (e.g. morning/08:00, evening/20:15) when
-- users can be asked to provide input regarding goal adherence
CREATE TABLE goal_adherence_checks (
    study_id BIGINT NOT NULL,
    check_id INT NOT NULL,
    title varchar NOT NULL,
    time TIME NOT NULL,

    PRIMARY KEY (study_id, check_id),
    FOREIGN KEY (study_id) REFERENCES studies(study_id) ON DELETE CASCADE
);

-- Allows to define for a goal template when users are ask about goal adherence
-- for a goal that is based on this template
CREATE TABLE goal_template_adherence_checks (
    study_id BIGINT NOT NULL,
    template_id INT NOT NULL,
    check_id INT NOT NULL,

    PRIMARY KEY (study_id, template_id, check_id),
    FOREIGN KEY (study_id, template_id) REFERENCES goal_templates(study_id, template_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, check_id) REFERENCES goal_adherence_checks(study_id, check_id) ON DELETE CASCADE
);

-- Allows to define study global configuration for goals
-- This has a 1:0..1 relation to the study
CREATE TABLE study_goal_config (
    study_id BIGINT NOT NULL,
    commitment TEXT,
    achievability TEXT,
    understandability TEXT,

    PRIMARY KEY (study_id),
    FOREIGN KEY (study_id) REFERENCES studies(study_id) ON DELETE CASCADE
);


-- a goal defined by a user based on a template --
CREATE TABLE goal (
    study_id BIGINT NOT NULL,
    goal_id INT NOT NULL,
    participant_id INT NOT NULL,
    template_id INT NOT NULL,
    properties JSONB NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),
    modified TIMESTAMP NOT NULL DEFAULT now(),

    PRIMARY KEY (study_id, goal_id),
    FOREIGN KEY (study_id, participant_id) REFERENCES participants(study_id, participant_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, template_id) REFERENCES goal_templates(study_id, template_id) ON DELETE CASCADE
);


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

