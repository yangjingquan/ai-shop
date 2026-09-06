package com.shop.presale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.presale.entity.PresalePayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PresalePaymentMapper extends BaseMapper<PresalePayment> {
    @Select("SELECT * FROM presale_payment WHERE transaction_id = #{transactionId} LIMIT 1")
    PresalePayment selectByTransactionId(@Param("transactionId") String transactionId);
}
