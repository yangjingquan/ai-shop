-- per_user_limit controls the per-user quota in the service layer.
-- The old unique key made any quota greater than one impossible to fulfill.
ALTER TABLE `user_coupon`
  DROP INDEX `uk_user_coupon_template`,
  ADD INDEX `idx_user_coupon_user_template` (`user_id`, `template_id`, `deleted`);
