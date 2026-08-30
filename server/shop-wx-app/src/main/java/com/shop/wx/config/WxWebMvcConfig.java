package com.shop.wx.config;

import com.shop.common.security.WxAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WxWebMvcConfig implements WebMvcConfigurer {

    private final WxAuthInterceptor wxAuthInterceptor;
    private final WxMerchantStatusInterceptor wxMerchantStatusInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(wxAuthInterceptor)
                .addPathPatterns("/api/wx/**")
                .excludePathPatterns("/api/wx/auth/**");
        registry.addInterceptor(wxMerchantStatusInterceptor)
                .addPathPatterns("/api/wx/**")
                .excludePathPatterns("/api/wx/auth/**");
    }
}
