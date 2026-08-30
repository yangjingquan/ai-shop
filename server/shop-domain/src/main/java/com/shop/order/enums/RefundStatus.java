package com.shop.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatus {
    PENDING(0, "待处理"),
    /** 商家同意，微信已受理，等待最终结果通知。 */
    REFUNDING(1, "退款处理中"),
    REJECTED(2, "已拒绝"),
    SUCCESS(3, "退款成功"),
    FAILED(4, "退款失败"),
    WAIT_RETURN_SHIP(5, "待填写退货物流"),
    WAIT_RETURN_RECEIVE(6, "待商家验货");

    private final int code;
    private final String text;
}
