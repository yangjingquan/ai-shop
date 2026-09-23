package com.shop.home.controller;

import com.shop.common.aop.OpLog;
import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.home.dto.StorefrontPageSaveRequest;
import com.shop.home.dto.StorefrontPageSummaryVO;
import com.shop.home.dto.StorefrontPageVO;
import com.shop.home.dto.StorefrontTemplateVO;
import com.shop.home.service.StorefrontPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchant/storefront")
@RequiredArgsConstructor
@RequirePermission("merchant:home:config")
public class MerchantStorefrontController {
    private final StorefrontPageService storefrontPageService;
    private Long merchantId() { return CurrentUserHolder.get().getMerchantId(); }

    @GetMapping("/home") public ApiResult<StorefrontPageVO> home() { return ApiResult.success(storefrontPageService.homeEditor(merchantId())); }
    @PutMapping("/home") @OpLog(action = "STOREFRONT_HOME_DRAFT", targetType = "STOREFRONT_PAGE") public ApiResult<Void> saveHome(@RequestBody @Valid StorefrontPageSaveRequest request) { storefrontPageService.saveHomeDraft(merchantId(), request); return ApiResult.success(null); }
    @PostMapping("/home/publish") @OpLog(action = "STOREFRONT_HOME_PUBLISH", targetType = "STOREFRONT_PAGE") public ApiResult<Void> publishHome() { storefrontPageService.publishHome(merchantId()); return ApiResult.success(null); }

    @GetMapping("/topics") public ApiResult<List<StorefrontPageSummaryVO>> topics() { return ApiResult.success(storefrontPageService.listTopics(merchantId())); }
    @GetMapping("/topics/{id}") public ApiResult<StorefrontPageVO> topic(@PathVariable Long id) { return ApiResult.success(storefrontPageService.topicEditor(merchantId(), id)); }
    @PostMapping("/topics") @OpLog(action = "STOREFRONT_TOPIC_CREATE", targetType = "STOREFRONT_PAGE") public ApiResult<StorefrontPageVO> createTopic(@RequestBody @Valid StorefrontPageSaveRequest request) { return ApiResult.success(storefrontPageService.createTopic(merchantId(), request)); }
    @PutMapping("/topics/{id}") @OpLog(action = "STOREFRONT_TOPIC_UPDATE", targetType = "STOREFRONT_PAGE") public ApiResult<StorefrontPageVO> updateTopic(@PathVariable Long id, @RequestBody @Valid StorefrontPageSaveRequest request) { return ApiResult.success(storefrontPageService.updateTopic(merchantId(), id, request)); }
    @PostMapping("/topics/{id}/publish") @OpLog(action = "STOREFRONT_TOPIC_PUBLISH", targetType = "STOREFRONT_PAGE") public ApiResult<Void> publishTopic(@PathVariable Long id) { storefrontPageService.publishTopic(merchantId(), id); return ApiResult.success(null); }
    @PostMapping("/topics/{id}/offline") @OpLog(action = "STOREFRONT_TOPIC_OFFLINE", targetType = "STOREFRONT_PAGE") public ApiResult<Void> offlineTopic(@PathVariable Long id) { storefrontPageService.offlineTopic(merchantId(), id); return ApiResult.success(null); }
    @GetMapping("/templates") public ApiResult<List<StorefrontTemplateVO>> templates() { return ApiResult.success(storefrontPageService.templates()); }
}
