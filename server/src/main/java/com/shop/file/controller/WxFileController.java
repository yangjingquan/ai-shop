package com.shop.file.controller;

import com.shop.common.response.ApiResult;
import com.shop.file.service.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/wx/file")
@RequiredArgsConstructor
public class WxFileController {

    private final LocalFileStorageService localFileStorageService;

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResult.success(Map.of("url", localFileStorageService.save(file)));
    }
}
