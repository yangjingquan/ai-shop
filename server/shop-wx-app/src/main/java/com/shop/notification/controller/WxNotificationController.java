package com.shop.notification.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.notification.dto.UserNotificationVO;
import com.shop.notification.service.UserNotificationService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wx/notifications")
@RequiredArgsConstructor
public class WxNotificationController {

    private final UserNotificationService notificationService;
    private final WxMerchantResolver wxMerchantResolver;

    @GetMapping
    public ApiResult<PageResult<UserNotificationVO>> page(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          HttpServletRequest request) {
        return ApiResult.success(notificationService.page(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(request), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResult<Map<String, Long>> unreadCount(HttpServletRequest request) {
        long count = notificationService.unreadCount(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(request));
        return ApiResult.success(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ApiResult<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        notificationService.markRead(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(request), id);
        return ApiResult.success(null);
    }

    @PostMapping("/read-all")
    public ApiResult<Void> markAllRead(HttpServletRequest request) {
        notificationService.markAllRead(CurrentUserHolder.get().getUserId(),
                wxMerchantResolver.requireActiveMerchant(request));
        return ApiResult.success(null);
    }
}
