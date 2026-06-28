UPDATE merchant
SET merchant_code = LPAD(id, 6, '0')
WHERE merchant_code IS NULL OR merchant_code = '';
