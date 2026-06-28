package com.shop.banner.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerVO {

    private Long id;

    private String imageUrl;

    private Integer linkType;

    private String linkValue;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;
}
