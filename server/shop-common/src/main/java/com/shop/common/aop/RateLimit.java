package com.shop.common.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key 前缀 */
    String key() default "";

    /** 时间窗口内允许的最大请求数 */
    int limit() default 5;

    /** 时间窗口（秒） */
    int windowSec() default 60;

    /** 统计维度 */
    By by() default By.IP;

    enum By { IP, USER }
}
