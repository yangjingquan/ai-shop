package com.shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class OrderDictionaryVO {
    private List<Item> orderTypes;
    private List<Item> states;
    private List<Item> fulfillmentMethods;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Integer code;
        private String text;
    }
}
