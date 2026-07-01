ALTER TABLE merchant
  ADD COLUMN merchant_code VARCHAR(6) DEFAULT '' COMMENT '商户代码，基于商户ID自动生成' AFTER id;

UPDATE merchant
SET merchant_code = LPAD(id, 6, '0')
WHERE merchant_code = '' OR merchant_code IS NULL;

ALTER TABLE merchant
  ADD UNIQUE KEY uk_merchant_code (merchant_code);
