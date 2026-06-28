package com.shop.file.controller;

import com.shop.common.response.ApiResult;
import com.shop.file.dto.UploadSignVO;
import com.shop.file.service.CosClient;
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

    private final CosClient cosClient;
    private final LocalFileStorageService localFileStorageService;

    @GetMapping("/upload-sign")
    public ApiResult<UploadSignVO> getSign(@RequestParam(defaultValue = "file") String fileName) {
        return ApiResult.success(cosClient.generateUploadSign(fileName));
    }

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResult.success(Map.of("url", localFileStorageService.save(file)));
    }

    @PostMapping("/upload/batch")
    public ApiResult<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(localFileStorageService.save(file));
        }
        return ApiResult.success(urls);
    }

    @DeleteMapping("/delete")
    public ApiResult<Void> delete(@RequestParam("url") String url) throws IOException {
        localFileStorageService.delete(url);
        return ApiResult.success();
    }
}
