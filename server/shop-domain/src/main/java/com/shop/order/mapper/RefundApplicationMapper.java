package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.order.entity.RefundApplication;
import com.shop.dashboard.dto.DailyAmountRow;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RefundApplicationMapper extends BaseMapper<RefundApplication> {

    @Select("SELECT * FROM refund_application WHERE out_refund_no <> '' " +
            "AND (status = 1 OR (auto_refund = 1 AND status IN (0, 4) AND refund_reconcile_attempts < 10)) " +
            "AND (refund_reconcile_at IS NULL OR refund_reconcile_at < DATE_SUB(NOW(), INTERVAL 1 MINUTE)) " +
            "ORDER BY COALESCE(refund_reconcile_at, created_at) ASC, id ASC LIMIT #{limit}")
    List<RefundApplication> selectPendingReconciliation(@Param("limit") int limit);

    @Update("UPDATE refund_application SET refund_reconcile_at = #{now}, " +
            "refund_reconcile_attempts = refund_reconcile_attempts + 1, " +
            "refund_reconcile_error = #{error}, updated_at = #{now} WHERE id = #{id}")
    int markReconcileAttempt(@Param("id") Long id, @Param("now") LocalDateTime now,
                             @Param("error") String error);

    @Select("SELECT DATE(refund_time) AS day, COUNT(*) AS count, COALESCE(SUM(refund_amount), 0) AS amount " +
            "FROM refund_application WHERE status = 3 AND refund_time >= #{from} " +
            "GROUP BY DATE(refund_time) ORDER BY day")
    List<DailyAmountRow> selectAdminDailyRefund(@Param("from") LocalDateTime from);
}
