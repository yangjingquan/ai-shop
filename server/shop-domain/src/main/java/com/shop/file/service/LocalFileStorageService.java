package com.shop.file.service;

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
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${shop.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${shop.file.public-prefix:/uploads}")
    private String publicPrefix;

    public String save(MultipartFile file) throws IOException {
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
        return normalizePrefix() + "/" + dateDir + "/" + fileName;
    }

    public void delete(String url) throws IOException {
        String prefix = normalizePrefix();
        if (!StringUtils.hasText(url) || !url.startsWith(prefix + "/")) {
            return;
        }
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String relative = url.substring(prefix.length() + 1);
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        Files.deleteIfExists(target);
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
