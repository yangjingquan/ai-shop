ALTER TABLE points_account
  ADD COLUMN total_points INT NOT NULL DEFAULT 0 COMMENT '用于会员等级判定的累计有效积分' AFTER balance;

ALTER TABLE points_ledger
  ADD COLUMN total_change INT NOT NULL DEFAULT 0 COMMENT '本次对累计有效积分的变更' AFTER balance_after,
  ADD COLUMN total_points_after INT DEFAULT NULL COMMENT '变更后的累计有效积分' AFTER total_change;

CREATE TABLE member_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  level_no INT NOT NULL,
  name VARCHAR(32) NOT NULL,
  min_total_points INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_level_no (merchant_id, level_no, deleted),
  UNIQUE KEY uk_member_level_points (merchant_id, min_total_points, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户会员等级区间';

UPDATE points_account account
LEFT JOIN (
  SELECT user_id, merchant_id, GREATEST(0, SUM(CASE
    WHEN source IN ('REGISTER', 'SIGN_IN', 'ORDER_PAY', 'LOTTERY_DRAW', 'REFUND') THEN change_value
    ELSE 0 END)) AS total_points
  FROM points_ledger
  WHERE deleted = 0
  GROUP BY user_id, merchant_id
) ledger ON ledger.user_id = account.user_id AND ledger.merchant_id = account.merchant_id
SET account.total_points = COALESCE(ledger.total_points, 0)
WHERE account.deleted = 0;
