package com.shop.lottery.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LotteryActivityVO {
    private Long id;
    private String name;
    private String themeImage;
    private String entryImage;
    private String ruleText;
    private LotteryCondition condition;
    private Integer dailyChances;
    private Integer usedToday;
    private Integer remainingChances;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private String statusText;
    private Boolean active;
    private Integer probabilityVersion;
    private List<LotteryPrizeVO> prizes;
}
