package com.shop.points.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @JsonIgnoreProperties(ignoreUnknown = true) public class PointsProductRequest {
 private Long productId; private Long skuId; private Long couponTemplateId; @NotBlank private String title; private String image;
 @NotNull @Min(1) private Integer pointsPrice; @NotNull @Min(0) private Integer stock; @Min(0) private Integer perUserLimit;
 private LocalDateTime validFrom; private LocalDateTime validTo; @NotNull private Integer status;
}
