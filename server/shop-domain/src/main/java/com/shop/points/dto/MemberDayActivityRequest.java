package com.shop.points.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalTime;
@Data @JsonIgnoreProperties(ignoreUnknown = true) public class MemberDayActivityRequest {
 @NotBlank private String name; @NotNull @Min(1) @Max(31) private Integer dayOfMonth; @NotNull private LocalTime startTime; @NotNull private LocalTime endTime;
 @NotNull private Integer doublePoints; private Long couponTemplateId; @NotNull private Integer productScopeType; private String productScopeIdsJson; @NotNull private Integer stackable; @NotNull private Integer status;
}
