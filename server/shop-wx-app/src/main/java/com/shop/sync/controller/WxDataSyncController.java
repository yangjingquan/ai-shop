package com.shop.sync.controller;

import com.shop.common.response.ApiResult;
import com.shop.sync.dto.DataVersionVO;
import com.shop.sync.service.DataSyncService;
import com.shop.wx.config.WxMerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx/sync")
@RequiredArgsConstructor
public class WxDataSyncController {
    private final DataSyncService dataSyncService;
    private final WxMerchantResolver merchantResolver;

    @GetMapping("/version")
    public ApiResult<DataVersionVO> version(@RequestParam(required = false) Long since, HttpServletRequest request) {
        return ApiResult.success(dataSyncService.versionSince(merchantResolver.currentMerchantId(request), since));
    }
}
