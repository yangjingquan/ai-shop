package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateVO {

    private String orderNo;

    private BigDecimal payAmount;

    private PayParams payParams;

    @Data
    public static class PayParams {

        private String appId;

        private String timeStamp;

        private String nonceStr;

        private String packageStr;

        private String signType;

        private String paySign;
    }
}
