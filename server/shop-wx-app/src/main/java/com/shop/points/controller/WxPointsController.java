package com.shop.points.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.points.dto.*;
import com.shop.points.service.PointsMemberService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/wx/points") @RequiredArgsConstructor
public class WxPointsController {
 private final PointsMemberService pointsService; private final WxMerchantResolver merchantResolver;
 private Long merchant(HttpServletRequest r){ return merchantResolver.requireActiveMerchant(r); } private Long user(){ return CurrentUserHolder.get().getUserId(); }
 @GetMapping("/profile") public ApiResult<PointsProfileVO> profile(HttpServletRequest r){return ApiResult.success(pointsService.profile(user(),merchant(r)));}
 @GetMapping("/ledger") public ApiResult<List<PointsLedgerVO>> ledger(@RequestParam(defaultValue="30") int limit,HttpServletRequest r){return ApiResult.success(pointsService.ledger(user(),merchant(r),limit));}
 @PostMapping("/sign-in") public ApiResult<PointsProfileVO> signIn(HttpServletRequest r){return ApiResult.success(pointsService.signIn(user(),merchant(r)));}
 @GetMapping("/mall") public ApiResult<List<PointsProductVO>> mall(HttpServletRequest r){return ApiResult.success(pointsService.mall(user(),merchant(r)));}
 @PostMapping("/redeem") public ApiResult<PointsRedeemVO> redeem(@RequestBody @Valid PointsRedeemRequest q,HttpServletRequest r){return ApiResult.success(pointsService.redeem(user(),merchant(r),q));}
 @GetMapping("/member-day") public ApiResult<MemberDayActivityVO> day(HttpServletRequest r){return ApiResult.success(pointsService.memberDay(merchant(r)));}
 @PostMapping("/member-day/receive-coupon") public ApiResult<Long> receiveMemberDayCoupon(HttpServletRequest r){return ApiResult.success(pointsService.receiveMemberDayCoupon(user(),merchant(r)));}
}
