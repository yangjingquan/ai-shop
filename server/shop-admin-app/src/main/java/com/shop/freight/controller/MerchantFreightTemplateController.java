package com.shop.freight.controller;
import com.shop.common.aop.OpLog; import com.shop.common.response.ApiResult; import com.shop.common.security.*; import com.shop.freight.dto.*; import com.shop.freight.service.FreightTemplateService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/merchant/freight-templates") @RequiredArgsConstructor
public class MerchantFreightTemplateController {
 private final FreightTemplateService service; private Long merchant(){return CurrentUserHolder.get().getMerchantId();}
 @GetMapping @RequirePermission("merchant:freight:view") public ApiResult<List<FreightTemplateVO>> list(){return ApiResult.success(service.list(merchant()));}
 @GetMapping("/{id}") @RequirePermission("merchant:freight:view") public ApiResult<FreightTemplateVO> get(@PathVariable Long id){return ApiResult.success(service.get(merchant(),id));}
 @PostMapping @OpLog(action="FREIGHT_TEMPLATE_CREATE",targetType="FREIGHT_TEMPLATE") @RequirePermission("merchant:freight:manage") public ApiResult<Long> create(@Valid @RequestBody FreightTemplateRequest r){return ApiResult.success(service.create(merchant(),r));}
 @PutMapping("/{id}") @OpLog(action="FREIGHT_TEMPLATE_UPDATE",targetType="FREIGHT_TEMPLATE",targetIdExpr="#id") @RequirePermission("merchant:freight:manage") public ApiResult<Void> update(@PathVariable Long id,@Valid @RequestBody FreightTemplateRequest r){service.update(merchant(),id,r);return ApiResult.success();}
 @DeleteMapping("/{id}") @RequirePermission("merchant:freight:manage") public ApiResult<Void> delete(@PathVariable Long id){service.delete(merchant(),id);return ApiResult.success();}
}
