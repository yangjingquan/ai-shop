-- Disable the historical well-known bootstrap password hashes.
-- Controlled first-login bootstrap is handled by the application through
-- SHOP_BOOTSTRAP_ADMIN_PASSWORD / SHOP_BOOTSTRAP_MERCHANT_PASSWORD.
UPDATE admin_user
SET password_hash = '!disabled-default-account!'
WHERE username = 'admin'
  AND password_hash = '$2a$10$rh5wna6Xhb3tKVmJK3EKpuZArt31w5oqNGiDzowuYqRYyGX8ay6FK';

UPDATE merchant_user
SET password_hash = '!disabled-default-account!'
WHERE username = 'merchant01'
  AND password_hash = '$2a$10$0VpzXiThga7aTISvjSOteuE9UKxIO6CQo/mpl5kk9jrpuiaaMiLIe';
