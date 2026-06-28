package com.shop.common.config;

import com.shop.common.security.AdminAuthInterceptor;
import com.shop.common.security.MerchantAuthInterceptor;
import com.shop.common.security.WxAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final MerchantAuthInterceptor merchantAuthInterceptor;
    private final WxAuthInterceptor wxAuthInterceptor;

    @Value("${shop.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**");
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**")
                .excludePathPatterns("/api/merchant/auth/**");
        registry.addInterceptor(wxAuthInterceptor)
                .addPathPatterns("/api/wx/**")
                .excludePathPatterns("/api/wx/auth/**");
    }
}
