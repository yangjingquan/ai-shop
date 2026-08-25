ALTER TABLE product
  ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核 1通过 2驳回' AFTER status,
  ADD COLUMN audit_reason VARCHAR(500) DEFAULT '' COMMENT '审核意见' AFTER audit_status,
  ADD COLUMN audited_by BIGINT UNSIGNED DEFAULT NULL COMMENT '审核管理员' AFTER audit_reason,
  ADD COLUMN audited_at DATETIME DEFAULT NULL COMMENT '审核时间' AFTER audited_by,
  ADD KEY idx_audit_status (audit_status, status, deleted);
