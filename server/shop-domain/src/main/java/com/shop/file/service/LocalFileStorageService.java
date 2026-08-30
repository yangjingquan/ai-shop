package com.shop.file.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shop.banner.entity.Banner;
import com.shop.banner.mapper.BannerMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.file.entity.FileAsset;
import com.shop.file.mapper.FileAssetMapper;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductSku;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${shop.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${shop.file.public-prefix:/uploads}")
    private String publicPrefix;

    private final FileAssetMapper fileAssetMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final BannerMapper bannerMapper;

    public String save(MultipartFile file) throws IOException {
        return save(file, "SYSTEM", 0L, null);
    }

    public String save(MultipartFile file, String ownerType, Long ownerId, Long merchantId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、gif、webp 图片");
        }

        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(dateDir).normalize();
        Files.createDirectories(targetDir);

        String fileName = UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(fileName).normalize();
        file.transferTo(target);
        String url = normalizePrefix() + "/" + dateDir + "/" + fileName;
        FileAsset asset = new FileAsset();
        asset.setPublicUrl(url);
        asset.setOwnerType(ownerType);
        asset.setOwnerId(ownerId);
        asset.setMerchantId(merchantId);
        fileAssetMapper.insert(asset);
        return url;
    }

    public void delete(String url, String ownerType, Long ownerId, Long merchantId) throws IOException {
        String prefix = normalizePrefix();
        if (!StringUtils.hasText(url) || !url.startsWith(prefix + "/")) {
            throw new BusinessException(ErrorCode.FILE_NOT_OWNED);
        }
        FileAsset asset = fileAssetMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileAsset>()
                .eq(FileAsset::getPublicUrl, url));
        if (asset == null || !ownerType.equals(asset.getOwnerType())
                || !ownerId.equals(asset.getOwnerId())
                || (merchantId != null && !merchantId.equals(asset.getMerchantId()))) {
            throw new BusinessException(ErrorCode.FILE_NOT_OWNED);
        }
        if (isReferenced(url)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "文件仍被商品或轮播图引用，不能删除");
        }
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String relative = url.substring(prefix.length() + 1);
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BusinessException(ErrorCode.FILE_NOT_OWNED);
        }
        Files.deleteIfExists(target);
        fileAssetMapper.deleteById(asset.getId());
    }

    private boolean isReferenced(String url) {
        long productCount = productMapper.selectCount(new QueryWrapper<Product>().and(w -> w
                .eq("main_image", url)
                .or()
                .apply("JSON_CONTAINS(images, JSON_QUOTE({0}), '$')", url)));
        if (productCount > 0) {
            return true;
        }
        long skuCount = productSkuMapper.selectCount(new QueryWrapper<ProductSku>().eq("image", url));
        if (skuCount > 0) {
            return true;
        }
        return bannerMapper.selectCount(new QueryWrapper<Banner>().eq("image_url", url)) > 0;
    }

    private String getExtension(String fileName) {
        String cleanName = StringUtils.cleanPath(fileName == null ? "" : fileName);
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            return "";
        }
        return cleanName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizePrefix() {
        String prefix = StringUtils.hasText(publicPrefix) ? publicPrefix : "/uploads";
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
