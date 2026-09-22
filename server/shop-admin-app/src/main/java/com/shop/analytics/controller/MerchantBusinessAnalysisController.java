package com.shop.analytics.controller;

import com.shop.analytics.dto.BusinessAnalysisVO;
import com.shop.analytics.service.BusinessAnalysisService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController @RequestMapping("/api/merchant/business-analysis") @RequiredArgsConstructor
public class MerchantBusinessAnalysisController {
    private final BusinessAnalysisService service;
    @GetMapping @RequirePermission("merchant:analysis:view") public ApiResult<BusinessAnalysisVO> report(@RequestParam(required=false) LocalDateTime from, @RequestParam(required=false) LocalDateTime to) {
        CurrentUser user = CurrentUserHolder.get(); if (user == null || user.getMerchantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return ApiResult.success(service.report(user.getMerchantId(), from, to));
    }
}
