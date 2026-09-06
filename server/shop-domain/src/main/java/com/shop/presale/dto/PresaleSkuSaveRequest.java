package com.shop.presale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresaleSkuSaveRequest {
    @NotNull private Long productId;
    @NotNull private Long skuId;
    /** 商品基础价由服务端从所选 SKU 的当前售价计算，保留该字段仅兼容旧客户端请求。 */
    @DecimalMin("0.01") private BigDecimal finalPrice;
    @NotNull @DecimalMin("0.01") private BigDecimal depositAmount;
    @NotNull @DecimalMin("0.01") private BigDecimal depositDeductionAmount;
    private BigDecimal balanceAmount;
    @NotNull private Integer userLimit;
}
