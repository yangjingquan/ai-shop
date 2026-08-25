package com.shop.groupbuy.dto;

import lombok.Data;

@Data
public class GroupBuyGroupVO {
    private Long id;
    private Long productId;
    private Integer requiredCount;
    private Integer paidCount;
    private Integer status;
    private String statusText;
    private Long expireAt;
}
