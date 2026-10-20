-- Observation attachments
--
-- Metadata of files attached to an observation (e.g. info-observation messages).
-- The file content lives on the filesystem under {more.attachments.dir}/{study_id}/{observation_id}/{attachment_id},
-- shared with the data-gateway which serves it to the app.

CREATE TABLE observation_attachments (
    attachment_id UUID NOT NULL PRIMARY KEY,
    study_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    filename VARCHAR NOT NULL,
    content_type VARCHAR NOT NULL,
    size_bytes BIGINT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT now(),

    FOREIGN KEY (study_id, observation_id) REFERENCES observations(study_id, observation_id) ON DELETE CASCADE
);

CREATE INDEX observation_attachments_observation ON observation_attachments(study_id, observation_id);
