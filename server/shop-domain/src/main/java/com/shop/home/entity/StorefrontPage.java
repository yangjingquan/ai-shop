package com.shop.home.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("storefront_page")
public class StorefrontPage extends BaseEntity {
    private Long merchantId;
    private String pageType;
    private String pageKey;
    private String slug;
    private String draftSlug;
    private String title;
    private String summary;
    private String coverImage;
    private String draftTitle;
    private String draftSummary;
    private String draftCoverImage;
    private String status;
    private String draftJson;
    private String publishedJson;
    private LocalDateTime publishedAt;
}
