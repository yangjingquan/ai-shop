package com.shop.home.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StorefrontPageSaveRequest {
    @NotBlank private String title;
    private String slug;
    private String summary;
    private String coverImage;
    @NotNull @Valid private StorefrontDocument document;
}
