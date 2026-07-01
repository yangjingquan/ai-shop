package com.shop.file.service.impl;

import com.shop.file.dto.UploadSignVO;
import com.shop.file.service.CosClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class StubCosClient implements CosClient {

    @Override
    public UploadSignVO generateUploadSign(String fileName) {
        UploadSignVO vo = new UploadSignVO();
        vo.setUrl("/mock-cos/upload/" + UUID.randomUUID().toString().substring(0, 8) + ".jpg");
        vo.setExpireAt(LocalDateTime.now().plusHours(1).toString());
        return vo;
    }
}
