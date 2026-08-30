package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.order.dto.AdminPaymentVO;
import com.shop.order.entity.PaymentLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface PaymentLogMapper extends BaseMapper<PaymentLog> {

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
