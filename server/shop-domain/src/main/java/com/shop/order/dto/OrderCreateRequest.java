package com.shop.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotEmpty
    private List<Long> cartItemIds;

    @NotNull
    private Long addressId;

    /** 可选；服务端会重新校验归属、有效期、门槛和商品范围。 */
    private Long couponId;

    private String bundleGroupId;

    private String remark;
}
