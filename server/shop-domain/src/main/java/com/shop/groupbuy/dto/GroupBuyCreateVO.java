package com.shop.groupbuy.dto;

import com.shop.order.dto.OrderCreateVO;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroupBuyCreateVO {
    private Long groupId;
    private String orderNo;
    private BigDecimal payAmount;
    private OrderCreateVO.PayParams payParams;
}
