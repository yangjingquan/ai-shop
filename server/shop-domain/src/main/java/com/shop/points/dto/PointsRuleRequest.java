package com.shop.points.dto;
import jakarta.validation.constraints.Min;
import lombok.Data;
@Data public class PointsRuleRequest {
 @Min(0) private Integer registerPoints; @Min(1) private Integer payAmountYuan; @Min(0) private Integer pointsPerYuan; @Min(0) private Integer signInPoints;
 @Min(0) private Integer validDays; @Min(1) private Integer deductionPerYuan; @Min(0) private Integer deductionMaxPoints; private Integer status;
}
