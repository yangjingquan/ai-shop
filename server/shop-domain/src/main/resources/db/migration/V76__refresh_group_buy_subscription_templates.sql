-- Keep existing per-merchant activity settings, but replace the three retired template IDs.
UPDATE merchant_marketing_feature
SET config_json = JSON_SET(
    CASE WHEN JSON_VALID(config_json) THEN config_json ELSE JSON_OBJECT() END,
    '$.formedTemplateId', '1TJtFqCAypEGy5YNcO3WUYHn_xzXghcdUTxegIHsVN0',
    '$.expiringTemplateId', 'HXpw3Je-cU96oprVfCEih93NCG30Pd8EX6zYL-bzEqs',
    '$.failedTemplateId', '6SP1uh9d55qix9-bI9BNNUQVev8VJ0kM4mvVQ-KFbis'
)
WHERE feature_code = 'GROUP_BUY' AND deleted = 0;
