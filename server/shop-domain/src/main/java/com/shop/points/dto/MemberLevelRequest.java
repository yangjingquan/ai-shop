package com.shop.points.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemberLevelRequest {
    @NotBlank @Size(max = 32) private String name;
    @Min(0) private Integer minTotalPoints;
}
