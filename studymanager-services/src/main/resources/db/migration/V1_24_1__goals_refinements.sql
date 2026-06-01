-- changeset yourname:20250521-01
-- description: Change FKs on goal_template_* tables from ON DELETE CASCADE to RESTRICT
--              This prevents deletion of GoalTopics and GoalAdherenceChecks that are in use.

-- =============================================
-- 1. Drop existing FKs (that had ON DELETE CASCADE)
-- =============================================

ALTER TABLE goal_template_adherence_checks
    DROP CONSTRAINT IF EXISTS goal_template_adherence_checks_study_id_check_id_fkey;

ALTER TABLE goal_template_topics
    DROP CONSTRAINT IF EXISTS goal_template_topics_study_id_key_fkey;

-- =============================================
-- 2. Re-add FKs with RESTRICT (default behavior = block delete)
-- =============================================

ALTER TABLE goal_template_adherence_checks
    ADD CONSTRAINT goal_template_adherence_checks_study_id_check_id_fkey
    FOREIGN KEY (study_id, check_id)
    REFERENCES goal_adherence_checks(study_id, check_id)
    ON DELETE RESTRICT;

ALTER TABLE goal_template_topics
    ADD CONSTRAINT goal_template_topics_study_id_key_fkey
    FOREIGN KEY (study_id, key)
    REFERENCES goal_topics(study_id, key)
    ON DELETE RESTRICT;

COMMENT ON CONSTRAINT goal_template_adherence_checks_study_id_check_id_fkey
    ON goal_template_adherence_checks IS 'Prevents deletion of a GoalAdherenceCheck if it is referenced by any GoalTemplate';

COMMENT ON CONSTRAINT goal_template_topics_study_id_key_fkey
    ON goal_template_topics IS 'Prevents deletion of a GoalTopic if it is used by any GoalTemplate';