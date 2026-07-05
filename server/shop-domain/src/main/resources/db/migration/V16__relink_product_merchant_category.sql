UPDATE product p
JOIN category c
    ON c.id = p.category_id
    AND c.deleted = 0
JOIN merchant_category mc
    ON mc.merchant_id = p.merchant_id
    AND mc.source_category_id = c.id
    AND mc.deleted = 0
SET p.category_id = mc.id,
    p.updated_at = CURRENT_TIMESTAMP
WHERE p.deleted = 0;
