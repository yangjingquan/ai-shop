package com.shop.lottery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lottery_draw_record")
public class LotteryDrawRecord extends BaseEntity {
    private String drawNo;
    private Long activityId;
    private Long merchantId;
    private Long userId;
    private LocalDate drawDate;
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private BigDecimal probabilitySnapshot;
    private Integer probabilityVersion;
    private String idempotencyKey;
}
