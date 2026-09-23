package com.shop.home.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Safe, typed page schema consumed by the mini program's built-in block renderer. */
@Data
public class StorefrontDocument {
    private List<Module> modules = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();

    @Data
    public static class Module {
        private String id;
        private String code;
        private String title;
        private String subtitle;
        private Integer enabled = 1;
        private Integer sortOrder;
        private String productSource;
        private Integer productLimit;
        private List<Long> topicPageIds = new ArrayList<>();
    }

    @Data
    public static class Block {
        private String id;
        private String type;
        private String title;
        private String body;
        private String imageUrl;
        private String buttonText;
        private String linkType;
        private String productSource;
        private Integer productLimit;
        private List<com.shop.product.dto.ProductListVO> products = new ArrayList<>();
    }
}
