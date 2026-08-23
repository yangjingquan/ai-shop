package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {
    private String merchantCode;
    private String name;
    private String logo;
    private String description;
    private String address;
    private String contactName;
    private String contactPhone;
    private String wxAppId;
    private String wxSecret;
    private String wxMchId;
    private String wxPayApiV3Key;
    private String wxPayMchSerialNo;
    private String wxPayPrivateKey;
    private String wxPayNotifyUrl;
    private Integer wxPayEnabled;
    private Integer status;
    private Long createdByAdminId;
}
