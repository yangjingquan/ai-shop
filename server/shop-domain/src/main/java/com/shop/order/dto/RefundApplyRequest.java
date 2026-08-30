package com.shop.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundApplyRequest {

    @Size(max = 200, message = "退款原因不能超过200字")
    private String reason;

    /** 为空表示全额退款；部分退款金额必须大于 0 且不超过实付金额。 */
    @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
    @Digits(integer = 12, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;
}
