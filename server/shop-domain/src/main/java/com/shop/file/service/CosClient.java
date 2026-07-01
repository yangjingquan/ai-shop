package com.shop.file.service;

import com.shop.file.dto.UploadSignVO;

public interface CosClient {
    UploadSignVO generateUploadSign(String fileName);
}
