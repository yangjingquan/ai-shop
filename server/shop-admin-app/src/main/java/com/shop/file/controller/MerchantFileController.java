package com.shop.file.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUserHolder;
import com.shop.file.service.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/file")
@RequiredArgsConstructor
public class MerchantFileController {

    private final LocalFileStorageService localFileStorageService;

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        return ApiResult.success(Map.of("url", localFileStorageService.save(file, "MERCHANT",
                merchantId, merchantId)));
    }

    @PostMapping("/upload/batch")
    public ApiResult<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            Long merchantId = CurrentUserHolder.get().getMerchantId();
            urls.add(localFileStorageService.save(file, "MERCHANT", merchantId, merchantId));
        }
        return ApiResult.success(urls);
    }

    @DeleteMapping("/delete")
    public ApiResult<Void> delete(@RequestParam("url") String url) throws IOException {
        Long merchantId = CurrentUserHolder.get().getMerchantId();
        localFileStorageService.delete(url, "MERCHANT", merchantId, merchantId);
        return ApiResult.success();
    }
}
