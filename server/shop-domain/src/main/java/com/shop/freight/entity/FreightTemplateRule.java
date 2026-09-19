package com.shop.freight.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data @EqualsAndHashCode(callSuper = true) @TableName("freight_template_rule")
public class FreightTemplateRule extends BaseEntity {
    private Long templateId; private String province; private String city; private Integer firstWeightGram;
    private BigDecimal firstFee; private Integer additionalWeightGram; private BigDecimal additionalFee;
    private BigDecimal freeThresholdAmount;
}
