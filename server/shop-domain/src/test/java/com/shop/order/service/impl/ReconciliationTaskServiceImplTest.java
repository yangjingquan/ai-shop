package com.shop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.entity.ReconciliationTask;
import com.shop.order.mapper.ReconciliationTaskMapper;
import com.shop.order.service.PaymentReconciliationService;
import com.shop.order.service.RefundReconciliationService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReconciliationTaskServiceImplTest {
    @Test
    void previewRejectsAnInvertedRange() {
        var service = new ReconciliationTaskServiceImpl(mock(ReconciliationTaskMapper.class), mock(PaymentReconciliationService.class), mock(RefundReconciliationService.class), new ObjectMapper());
        LocalDateTime now = LocalDateTime.now();
        assertThrows(RuntimeException.class, () -> service.preview("REFUND", null, now, now.minusMinutes(1)));
    }

    @Test
    void scopedPaymentTaskRecordsTheScopedResult() {
        ReconciliationTaskMapper mapper = mock(ReconciliationTaskMapper.class);
        PaymentReconciliationService payment = mock(PaymentReconciliationService.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(payment.reconcilePending(eq(100), eq(9L), any(), any())).thenReturn(3);
        var service = new ReconciliationTaskServiceImpl(mapper, payment, mock(RefundReconciliationService.class), new ObjectMapper());
        LocalDateTime from = LocalDateTime.now().minusHours(1), to = LocalDateTime.now();
        var result = service.createAndRun("PAYMENT", 9L, from, to, "1");
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(3, result.getAffectedCount());
        verify(payment).reconcilePending(100, 9L, from, to);
        verify(mapper, atLeastOnce()).insert(any(ReconciliationTask.class));
    }
}
