-- 普通购物车行与搭配购套餐行可以使用同一个 SKU，但同一套餐分组内仍不能重复添加同一 SKU。
ALTER TABLE cart_item
  DROP INDEX uk_cart_user_sku_active,
  DROP COLUMN active_unique_key,
  ADD COLUMN active_unique_key VARCHAR(64)
    GENERATED ALWAYS AS (
      CASE
        WHEN deleted = 0 THEN COALESCE(NULLIF(bundle_group_id, ''), '__NORMAL__')
        ELSE NULL
      END
    ) STORED,
  ADD UNIQUE KEY uk_cart_user_sku_active (user_id, sku_id, active_unique_key);
