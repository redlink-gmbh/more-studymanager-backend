-- Update gateway-view
CREATE OR REPLACE VIEW auth_routing_info (api_id, api_secret, study_id, participant_id, study_group_id, study_is_active) AS
SELECT api_credentials.*, pt.study_group_id, s.status = 'active'
FROM api_credentials
         INNER JOIN participants pt
                    ON (api_credentials.study_id = pt.study_id and api_credentials.participant_id = pt.participant_id)
         INNER JOIN studies s
                    ON (api_credentials.study_id = s.study_id)
;
