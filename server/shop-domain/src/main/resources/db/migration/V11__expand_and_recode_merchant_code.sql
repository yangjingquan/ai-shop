ALTER TABLE merchant
  DROP INDEX uk_merchant_code;

ALTER TABLE merchant
  MODIFY COLUMN merchant_code VARCHAR(32) NOT NULL DEFAULT '' COMMENT '商户代码，安全随机生成的对外商户编号';

UPDATE merchant
SET merchant_code = CONCAT('M', LPAD(UPPER(HEX(id)), 10, '0'))
WHERE merchant_code IS NULL
   OR merchant_code = ''
   OR merchant_code REGEXP '^[0-9]+$';

ALTER TABLE merchant
  ADD UNIQUE KEY uk_merchant_code (merchant_code);
