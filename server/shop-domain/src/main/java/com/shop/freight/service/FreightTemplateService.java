package com.shop.freight.service;
import com.shop.freight.dto.*; import com.shop.product.entity.Product; import com.shop.product.entity.ProductSku; import java.math.BigDecimal; import java.util.*;
public interface FreightTemplateService { List<FreightTemplateVO> list(Long merchantId); FreightTemplateVO get(Long merchantId, Long id); Long create(Long merchantId, FreightTemplateRequest request); void update(Long merchantId, Long id, FreightTemplateRequest request); void delete(Long merchantId, Long id); FreightQuote quote(Long merchantId, String region, List<Line> lines, BigDecimal activityDiscount);
 record Line(Product product, ProductSku sku, int quantity, BigDecimal amount) {} }
