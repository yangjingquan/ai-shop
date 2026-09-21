package com.shop.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class CustomerOperationRequest {
    private CustomerOperationRequest() {}

    @Data
    public static class TagSave {
        @NotBlank private String name;
        private String color;
        @NotNull private Integer status;
    }

    @Data
    public static class TagBinding {
        @NotEmpty private List<Long> userIds = new ArrayList<>();
        @NotNull private Integer status;
    }

    @Data
    public static class SegmentSave {
        @NotBlank private String name;
        private String description;
        @NotBlank private String segmentType;
        @Valid @NotNull private SegmentConditionGroup condition;
        @NotNull private Integer status;
    }

    @Data
    public static class SegmentConditionGroup {
        private String logic = "AND";
        @NotEmpty @Valid private List<SegmentCondition> conditions = new ArrayList<>();
    }

    @Data
    public static class SegmentCondition {
        @NotBlank private String field;
        @NotBlank private String operator;
        @NotBlank private String value;
    }
}
