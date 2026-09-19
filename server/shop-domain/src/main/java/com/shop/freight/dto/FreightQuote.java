package com.shop.freight.dto;
import lombok.Data; import java.math.BigDecimal; import java.util.*;
@Data public class FreightQuote { private BigDecimal amount = BigDecimal.ZERO; private List<Item> items = new ArrayList<>();
 @Data public static class Item { private Long templateId; private Long version; private int weightGram; private BigDecimal merchandiseAmount; private BigDecimal activityDiscountAmount; private BigDecimal freightAmount; private boolean freeShipping; private String matchedRegion; }}
