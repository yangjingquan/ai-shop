package com.shop.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_marketing_feature")
public class MerchantMarketingFeature extends BaseEntity {
    private Long merchantId;
    private String featureCode;
    private Integer enabled;
    private String configJson;
    private Integer sort;
    private Long updatedBy;
    private Integer version;
}
