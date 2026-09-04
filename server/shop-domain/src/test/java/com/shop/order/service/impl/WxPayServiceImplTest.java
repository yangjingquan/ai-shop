package com.shop.order.service.impl;

import com.wechat.pay.java.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxPayServiceImplTest {

    @Test
    void formatsWxPayDateTimeWithoutFractionalSeconds() {
        OffsetDateTime value = OffsetDateTime.of(2026, 8, 30, 11, 47, 18,
                901892894, ZoneOffset.ofHours(8));

        assertEquals("2026-08-30T11:47:18+08:00", WxPayServiceImpl.formatWxPayDateTime(value));
    }

    @Test
    void derivesRefundNotifyUrlFromValidatedPaymentNotifyUrl() {
        assertEquals("https://pay.example.com/api/callback/wxrefund/M0001",
                WxPayServiceImpl.refundNotifyUrl("https://pay.example.com/api/callback/wxpay/M0001"));
    }

    @Test
    void recognizesWechatOrderNotFoundOnlyForHttp404() {
        ServiceException notFound = new ServiceException(null, 404,
                "{\"code\":\"ORDER_NOT_EXIST\",\"message\":\"订单不存在\"}");
        ServiceException other404 = new ServiceException(null, 404,
                "{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统错误\"}");

        assertTrue(WxPayServiceImpl.isOrderNotFound(notFound));
        assertFalse(WxPayServiceImpl.isOrderNotFound(other404));
    }
}
