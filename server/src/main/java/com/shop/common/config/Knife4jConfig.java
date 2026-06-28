package com.shop.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "knife4j.enabled", havingValue = "true", matchIfMissing = false)
public class Knife4jConfig {

    @Bean
    public OpenAPI shopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("商城 MVP API")
                        .version("1.0.0")
                        .description("多商家电商小程序"));
    }
}
