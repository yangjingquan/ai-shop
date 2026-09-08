package com.shop.sync.web;

import com.shop.common.security.CurrentUserHolder;
import com.shop.sync.dto.DataVersionVO;
import com.shop.sync.service.DataSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Publishes one invalidation event for every successful business write and exposes freshness headers to every consumer. */
@Component
@RequiredArgsConstructor
public class DataSyncInterceptor implements HandlerInterceptor {
    private final DataSyncService dataSyncService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
        Long merchantId = CurrentUserHolder.get() == null ? null : CurrentUserHolder.get().getMerchantId();
        if (!HttpMethod.GET.matches(request.getMethod()) && response.getStatus() < 400) {
            dataSyncService.invalidate(scope(request.getRequestURI()), merchantId, "", "INVALIDATE");
        }
        Long known = null;
        try { known = Long.valueOf(request.getHeader("x-shop-data-version")); } catch (Exception ignored) { }
        DataVersionVO version = dataSyncService.versionSince(merchantId, known);
        response.setHeader("X-Shop-Data-Version", String.valueOf(version.getDataVersion()));
        response.setHeader("X-Shop-Server-Time", version.getServerTime().toString());
        response.setHeader("X-Shop-Invalidated-Scopes", String.join(",", version.getInvalidatedScopes()));
    }

    private String scope(String uri) {
        if (uri.contains("banner")) return "BANNER";
        if (uri.contains("product")) return "PRODUCT";
        if (uri.contains("inventory") || uri.contains("stock")) return "INVENTORY";
        if (uri.contains("coupon")) return "COUPON";
        if (uri.contains("marketing") || uri.contains("seckill") || uri.contains("presale") || uri.contains("promotion")) return "MARKETING";
        if (uri.contains("merchant")) return "MERCHANT";
        return "ORDER";
    }
}
