package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.order.dto.AdminPaymentVO;
import com.shop.order.entity.PaymentLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.shop.dashboard.dto.DailyAmountRow;
import java.util.List;

@Mapper
public interface PaymentLogMapper extends BaseMapper<PaymentLog> {

    @Select("<script>SELECT COUNT(*) FROM payment_log pl " +
            "JOIN `order` o ON o.order_no = pl.order_no AND o.deleted = 0 " +
            "WHERE pl.created_at &gt;= #{from} AND pl.created_at &lt; #{to} " +
            "<if test='merchantId != null'>AND o.merchant_id = #{merchantId}</if>" +
            "</script>")
    Long selectPaidCount(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                         @Param("merchantId") Long merchantId);

    @Select("<script>SELECT COALESCE(SUM(pl.amount), 0) FROM payment_log pl " +
            "JOIN `order` o ON o.order_no = pl.order_no AND o.deleted = 0 " +
            "WHERE pl.created_at &gt;= #{from} AND pl.created_at &lt; #{to} " +
            "<if test='merchantId != null'>AND o.merchant_id = #{merchantId}</if>" +
            "</script>")
    BigDecimal selectPaidAmount(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                @Param("merchantId") Long merchantId);

    @Select("<script>SELECT DATE(pl.created_at) AS day, COUNT(*) AS count, " +
            "COALESCE(SUM(pl.amount), 0) AS amount " +
            "FROM payment_log pl JOIN `order` o ON o.order_no = pl.order_no AND o.deleted = 0 " +
            "WHERE pl.created_at &gt;= #{from} AND pl.created_at &lt; #{to} " +
            "GROUP BY DATE(pl.created_at) ORDER BY day</script>")
    List<DailyAmountRow> selectAdminDailyPaid(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Select("<script>" +
            "SELECT pl.id, pl.order_no, pl.transaction_id, pl.amount, pl.created_at, " +
            "o.merchant_id, m.name AS merchant_name, o.status AS order_status, o.pay_time, " +
            "o.pay_reconcile_at, o.pay_reconcile_attempts, o.pay_reconcile_error " +
            "FROM payment_log pl JOIN `order` o ON o.order_no = pl.order_no AND o.deleted = 0 " +
            "LEFT JOIN merchant m ON m.id = o.merchant_id AND m.deleted = 0 WHERE 1=1 " +
            "<if test='merchantId != null'>AND o.merchant_id = #{merchantId} </if>" +
            "<if test='orderNo != null and orderNo != \"\"'>AND pl.order_no LIKE CONCAT('%', #{orderNo}, '%') </if>" +
            "<if test='transactionId != null and transactionId != \"\"'>AND pl.transaction_id LIKE CONCAT('%', #{transactionId}, '%') </if>" +
            "<if test='createdFrom != null'>AND pl.created_at &gt;= #{createdFrom} </if>" +
            "<if test='createdTo != null'>AND pl.created_at &lt; #{createdTo} </if>" +
            "ORDER BY pl.id DESC" +
            "</script>")
    IPage<AdminPaymentVO> selectAdminPage(Page<AdminPaymentVO> page,
            @Param("merchantId") Long merchantId,
            @Param("orderNo") String orderNo,
            @Param("transactionId") String transactionId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);
}
