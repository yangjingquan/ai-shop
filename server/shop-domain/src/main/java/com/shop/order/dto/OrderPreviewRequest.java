package com.shop.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewRequest {

    @NotEmpty
    private List<Long> cartItemIds;

    @NotNull
    private Long addressId;

    /** 可选；为空时服务端自动推荐当前订单可用的最优新人券。 */
    private Long couponId;
}
