package com.shop.lottery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lottery_activity")
public class LotteryActivity extends BaseEntity {
    private Long merchantId;
    private String name;
    private String themeImage;
    private String entryImage;
    private String ruleText;
    private String conditionJson;
    private Integer dailyChances;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private Integer probabilityVersion;
}
