package com.shop.admin.config;

import com.shop.common.security.AdminAuthInterceptor;
import com.shop.common.security.MerchantAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final MerchantAuthInterceptor merchantAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**");
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**")
                .excludePathPatterns("/api/merchant/auth/**");
    }
}
