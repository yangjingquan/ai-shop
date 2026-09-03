package com.shop.groupbuy.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupBuySubscriptionConfigVO {
    private List<Template> templates;

    @Data
    public static class Template {
        private String templateType;
        private String templateId;

        public Template() {}

        public Template(String templateType, String templateId) {
            this.templateType = templateType;
            this.templateId = templateId;
        }
    }
}
