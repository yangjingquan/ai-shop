package com.shop.analytics.controller;
import com.shop.analytics.dto.BusinessAnalysisVO;
import com.shop.analytics.service.BusinessAnalysisService;
import com.shop.common.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@RestController @RequestMapping("/api/admin/business-analysis") @RequiredArgsConstructor
public class AdminBusinessAnalysisController { private final BusinessAnalysisService service;
    @GetMapping public ApiResult<BusinessAnalysisVO> report(@RequestParam(required=false) Long merchantId, @RequestParam(required=false) LocalDateTime from, @RequestParam(required=false) LocalDateTime to) { return ApiResult.success(service.report(merchantId, from, to)); }
}
