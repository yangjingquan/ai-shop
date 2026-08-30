package com.shop.merchant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantWechatSettingsVO {
    private Long merchantId;
    private String merchantCode;
    private String merchantName;
    private String wxAppId;
    private String wxMchId;
    private Boolean wxSecretConfigured;
    private String wxPayMchSerialNo;
    private String wxPayPublicKeyId;
    private String wxPayNotifyUrl;
    private Integer wxPayEnabled;
    private Boolean wxPayApiV3KeyConfigured;
    private Boolean wxPayPrivateKeyConfigured;
    private Boolean wxPayPublicKeyConfigured;
    private Boolean wxPayConfigured;
    private LocalDateTime updatedAt;
}
