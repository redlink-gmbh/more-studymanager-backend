-- Marks a milestone as essential for a correct study configuration: it should be set for every participant (not enforced)
ALTER TABLE milestones
    ADD COLUMN essential BOOLEAN NOT NULL DEFAULT FALSE;
