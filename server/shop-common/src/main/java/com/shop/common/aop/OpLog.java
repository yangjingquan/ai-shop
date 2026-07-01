package com.shop.common.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 操作类型 */
    String action();

    /** SpEL 表达式取目标 ID，例 #orderNo */
    String targetIdExpr() default "";

    /** 目标类型，例 ORDER / MERCHANT / REFUND */
    String targetType() default "";
}
