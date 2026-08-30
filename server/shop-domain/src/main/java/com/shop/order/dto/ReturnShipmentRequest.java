package com.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnShipmentRequest {

    @NotBlank(message = "退货承运商不能为空")
    @Size(max = 64, message = "退货承运商不能超过64字")
    private String shipCompany;

    @NotBlank(message = "退货单号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{5,30}$", message = "退货单号格式不合法")
    private String shipNo;
}
