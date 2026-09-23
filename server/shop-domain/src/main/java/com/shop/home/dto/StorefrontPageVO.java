package com.shop.home.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StorefrontPageVO {
    private Long id;
    private String pageType;
    private String title;
    private String slug;
    private String summary;
    private String coverImage;
    private String status;
    private LocalDateTime publishedAt;
    private StorefrontDocument draft;
    private StorefrontDocument published;
    private boolean draftChanged;
}
