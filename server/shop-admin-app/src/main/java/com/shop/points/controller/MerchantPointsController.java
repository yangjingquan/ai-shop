package com.shop.points.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.points.dto.*;
import com.shop.points.service.PointsMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/merchant/points") @RequiredArgsConstructor
public class MerchantPointsController {
 private final PointsMemberService pointsService; private Long merchant(){CurrentUser u=CurrentUserHolder.get();return u==null?null:u.getMerchantId();}
 @GetMapping("/rule") @RequirePermission("merchant:points:view") public ApiResult<PointsRuleRequest> rule(){return ApiResult.success(pointsService.rule(merchant()));}
 @PutMapping("/rule") @RequirePermission("merchant:points:update") public ApiResult<Void> saveRule(@RequestBody @Valid PointsRuleRequest q){pointsService.saveRule(merchant(),q);return ApiResult.success();}
 @GetMapping("/products") @RequirePermission("merchant:points:view") public ApiResult<List<PointsProductVO>> products(){return ApiResult.success(pointsService.merchantProducts(merchant()));}
 @PostMapping("/products") @RequirePermission("merchant:points:update") public ApiResult<Long> create(@RequestBody @Valid PointsProductRequest q){return ApiResult.success(pointsService.saveProduct(merchant(),null,q));}
 @PutMapping("/products/{id}") @RequirePermission("merchant:points:update") public ApiResult<Long> update(@PathVariable Long id,@RequestBody @Valid PointsProductRequest q){return ApiResult.success(pointsService.saveProduct(merchant(),id,q));}
 @DeleteMapping("/products/{id}") @RequirePermission("merchant:points:update") public ApiResult<Void> delete(@PathVariable Long id){pointsService.deleteProduct(merchant(),id);return ApiResult.success();}
 @GetMapping("/member-day") @RequirePermission("merchant:points:view") public ApiResult<MemberDayActivityVO> day(){return ApiResult.success(pointsService.merchantMemberDay(merchant()));}
 @PutMapping("/member-day") @RequirePermission("merchant:points:update") public ApiResult<Void> saveDay(@RequestBody @Valid MemberDayActivityRequest q){pointsService.saveMemberDay(merchant(),q);return ApiResult.success();}
}
