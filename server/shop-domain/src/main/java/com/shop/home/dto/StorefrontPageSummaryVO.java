package com.shop.home.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StorefrontPageSummaryVO {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String coverImage;
    private String status;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
