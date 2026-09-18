package com.shop.file.service;

import com.shop.banner.mapper.BannerMapper;
import com.shop.file.mapper.FileAssetMapper;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileStorageServiceTest {

    @TempDir
    Path uploadDir;

    @Test
    void savesWechatAvatarWhenMultipartFilenameHasNoExtension() throws Exception {
        FileAssetMapper fileAssetMapper = mock(FileAssetMapper.class);
        when(fileAssetMapper.insert(any())).thenReturn(1);
        LocalFileStorageService service = new LocalFileStorageService(
                fileAssetMapper, mock(ProductMapper.class), mock(ProductSkuMapper.class), mock(BannerMapper.class));
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(service, "publicPrefix", "/uploads");

        MockMultipartFile avatar = new MockMultipartFile("file", "avatar", "image/png", pngBytes());

        String url = service.save(avatar, "USER", 1L, 1L);

        assertTrue(url.matches("/uploads/\\d{8}/[0-9a-f-]+\\.png"));
        assertTrue(Files.exists(uploadDir.resolve(url.substring("/uploads/".length()))));
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
