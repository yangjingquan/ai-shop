ALTER TABLE banner
  ADD COLUMN merchant_id BIGINT UNSIGNED NULL COMMENT '所属商家，NULL 表示平台 Banner' AFTER id,
  ADD KEY idx_merchant_status_sort (merchant_id, status, sort, deleted);

UPDATE banner b
JOIN (
  SELECT MIN(id) AS id
  FROM merchant
  WHERE deleted = 0
) m ON 1 = 1
SET b.merchant_id = m.id
WHERE b.merchant_id IS NULL;
