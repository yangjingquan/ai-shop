package com.shop.pricing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("marketing_rule_audit")
public class MarketingRuleAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleVersion;
    private Long operatorId;
    private String action;
    private String reason;
    private String beforeJson;
    private String afterJson;
}
