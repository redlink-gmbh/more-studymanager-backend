CREATE TABLE IF NOT EXISTS nvpairs_observations (
    study_id BIGINT NOT NULL,
    observation_id INT,
    name VARCHAR,
    value bytea NOT NULL,

    PRIMARY KEY (study_id, observation_id, name),
    FOREIGN KEY (study_id, observation_id) REFERENCES observations(study_id, observation_id) ON DELETE CASCADE
);

INSERT INTO nvpairs_observations (name, value, study_id, observation_id)
SELECT
    name,
    value,
    CAST(substring(issuer, '^\d+') AS BIGINT) AS study_id,
    CAST(substring(replace(issuer, 'null', '0'), '^\d+-\d+-(\d+)') AS INT) AS observation_id
FROM nvpairs
WHERE issuer LIKE '%_observation'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS nvpairs_triggers (
    study_id BIGINT NOT NULL,
    intervention_id INT,
    name VARCHAR,
    value bytea NOT NULL,

    PRIMARY KEY (study_id, intervention_id, name),
    FOREIGN KEY (study_id, intervention_id) REFERENCES interventions(study_id, intervention_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, intervention_id) REFERENCES triggers(study_id, intervention_id) ON DELETE CASCADE
);

INSERT INTO nvpairs_triggers (name, value, study_id, intervention_id)
SELECT
    name,
    value,
    CAST(substring(issuer, '^\d+') AS BIGINT) AS study_id,
    CAST(substring(replace(issuer, 'null', '0'), '^\d+-\d+-(\d+)') AS INT) AS intervention_id
FROM nvpairs
WHERE issuer LIKE '%_trigger'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS nvpairs_actions (
    study_id BIGINT NOT NULL,
    intervention_id INT,
    action_id INT,
    name VARCHAR,
    value bytea NOT NULL,

    PRIMARY KEY (study_id, intervention_id, action_id, name),
    FOREIGN KEY (study_id, intervention_id) REFERENCES interventions(study_id, intervention_id) ON DELETE CASCADE,
    FOREIGN KEY (study_id, intervention_id, action_id) REFERENCES actions(study_id, intervention_id, action_id) ON DELETE CASCADE
);

INSERT INTO nvpairs_actions (name, value, study_id, intervention_id, action_id)
SELECT
    name,
    value,
    CAST(substring(issuer, '^\d+') AS BIGINT) AS study_id,
    CAST(substring(replace(issuer, 'null', '0'), '^\d+-\d+-(\d+)') AS INT) AS intervention_id,
    CAST(substring(replace(issuer, 'null', '0'), '^\d+-\d+-\d+-(\d+)') AS INT) AS action_id
FROM nvpairs
WHERE issuer LIKE '%_action'
ON CONFLICT DO NOTHING;

DROP TABLE nvpairs;