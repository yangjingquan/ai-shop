package com.shop.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    PARAM_ERROR(100, "参数错误"),
    BIZ_ERROR(101, "业务规则错误"),
    STOCK_NOT_ENOUGH(110, "库存不足"),
    PRODUCT_OFF_SHELF(111, "商品已下架"),
    PRODUCT_NOT_FOUND(112, "商品不存在"),
    CART_FULL(120, "购物车已满"),
    CART_ITEM_EMPTY(121, "请选择要结算的商品"),
    CART_ITEM_NOT_OWNED(122, "购物车项不存在或不属于当前用户"),
    CART_ITEM_INVALID(123, "购物车存在失效商品,请刷新"),
    CART_ITEM_CROSS_MERCHANT(124, "不支持跨商家下单"),
    ORDER_STATUS_ERROR(130, "订单状态不允许此操作"),
    ORDER_NOT_FOUND(131, "订单不存在"),
    ORDER_STATUS_NOT_ALLOWED(132, "当前订单状态不允许此操作"),
    ORDER_EXPIRED(133, "订单已超时取消"),
    PAY_FAILED(140, "微信支付失败"),
    WX_LOGIN_FAILED(141, "微信登录失败"),
    SHIP_NO_INVALID(142, "物流单号格式不合法"),
    ORDER_NOT_YOUR_MERCHANT(143, "订单不属于当前商家"),
    ORDER_NOT_WAIT_SHIP(144, "当前状态不可发货"),
    ORDER_NOT_WAIT_RECEIVE(145, "当前状态不可确认收货"),
    ORDER_NOT_REPAYABLE(146, "订单不可重新支付"),
    LOGIN_FAILED(150, "用户名或密码错误"),
    MERCHANT_FROZEN(151, "商家已被冻结"),
    USERNAME_EXISTS(160, "用户名已存在"),
    MERCHANT_NOT_FOUND(161, "商家不存在"),
    ADDRESS_NOT_FOUND(170, "收货地址不存在"),
    ADDRESS_LIMIT_EXCEEDED(171, "收货地址数量超限"),
    BIND_PHONE_FAILED(180, "手机号绑定失败"),
    REFUND_NOT_FOUND(190, "退款申请不存在"),
    REFUND_ALREADY_EXISTS(191, "该订单已有未处理的退款申请"),
    REFUND_ORDER_NOT_REFUNDABLE(192, "当前订单状态不可申请退款"),
    REFUND_NOT_PENDING(193, "退款申请已处理"),
    REFUND_NOT_YOUR_MERCHANT(194, "退款申请不属于当前商家"),
    CATEGORY_NOT_FOUND(200, "分类不存在"),
    CATEGORY_HAS_CHILDREN(201, "分类下还有子分类或商品，不能删"),
    SKU_LIMIT_EXCEEDED(211, "SKU 数量超限"),
    INVALID_SPEC(212, "规格定义不合法"),
    BANNER_NOT_FOUND(220, "轮播图不存在"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(500, "系统错误");

    private final int code;
    private final String msg;
}
