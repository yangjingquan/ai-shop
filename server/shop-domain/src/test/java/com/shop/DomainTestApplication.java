package com.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.shop")
@MapperScan("com.shop.**.mapper")
public class DomainTestApplication {
}
