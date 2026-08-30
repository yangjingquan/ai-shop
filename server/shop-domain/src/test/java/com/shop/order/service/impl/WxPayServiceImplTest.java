package com.shop.order.service.impl;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
