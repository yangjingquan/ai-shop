package com.shop.freight.dto;
import lombok.Data; import java.math.BigDecimal; import java.util.*;
@Data public class FreightTemplateVO { private Long id; private String name; private Integer enabled; private Long version; private List<Rule> rules = new ArrayList<>();
 @Data public static class Rule { private Long id; private String province; private String city; private Integer firstWeightGram; private BigDecimal firstFee; private Integer additionalWeightGram; private BigDecimal additionalFee; private BigDecimal freeThresholdAmount; }}
