package com.shop.freight.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data public class FreightTemplateRequest {
 @NotBlank @Size(max=64) private String name; @NotNull private Integer enabled = 1; @NotEmpty @Valid private List<Rule> rules;
 @Data public static class Rule { private String province; private String city; @NotNull @Min(1) private Integer firstWeightGram;
  @NotNull @DecimalMin("0.00") private BigDecimal firstFee; @NotNull @Min(1) private Integer additionalWeightGram;
  @NotNull @DecimalMin("0.00") private BigDecimal additionalFee; @DecimalMin("0.00") private BigDecimal freeThresholdAmount; }
}
