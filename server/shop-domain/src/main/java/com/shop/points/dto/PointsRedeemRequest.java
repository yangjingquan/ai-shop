package com.shop.points.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class PointsRedeemRequest { @NotNull private Long pointsProductId; @NotNull @Min(1) @Max(99) private Integer quantity; private Long addressId; }
