package com.shop.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Fulfilment is independent from how an order was priced or acquired. */
@Getter
@AllArgsConstructor
public enum FulfillmentMethod {
    NONE(0, "无需履约"),
    EXPRESS(1, "快递发货");

    private final int code;
    private final String text;

    public static FulfillmentMethod fromCode(Integer code) {
        if (code != null) {
            for (FulfillmentMethod value : values()) {
                if (value.code == code) return value;
            }
        }
        return EXPRESS;
    }
}
