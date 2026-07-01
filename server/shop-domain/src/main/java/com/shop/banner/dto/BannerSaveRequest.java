package com.shop.banner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BannerSaveRequest {

    @NotBlank
    private String imageUrl;

    @NotNull
    private Integer linkType;

    private String linkValue;

    @NotNull
    private Integer sort;

    @NotNull
    private Integer status;
}
