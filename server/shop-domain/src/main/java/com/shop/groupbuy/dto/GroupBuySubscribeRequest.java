package com.shop.groupbuy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GroupBuySubscribeRequest {
    @NotNull
    private Long groupId;

    @NotEmpty
    @Valid
    private List<Item> subscriptions;

    @Data
    public static class Item {
        @NotNull
        private String templateType;
        @NotNull
        private String templateId;
        @NotNull
        private String status;
    }
}
