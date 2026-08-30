package com.shop.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_wechat_config")
public class MerchantWechatConfig extends BaseEntity {
    private Long merchantId;
    private String wxAppId;
    private String wxSecret;
    private String wxMchId;
    private String wxPayApiV3Key;
    private String wxPayMchSerialNo;
    private String wxPayPrivateKey;
    private String wxPayPublicKey;
    private String wxPayPublicKeyId;
    private String wxPayNotifyUrl;
    private Integer wxPayEnabled;
}
