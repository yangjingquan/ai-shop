package com.shop.lottery.dto;

import lombok.Data;

@Data
public class LotteryCondition {
    private Integer minMemberLevel = 1;
    private Integer minPoints = 0;
    private Boolean firstOrderOnly = false;
    private Boolean repurchaseOnly = false;
}
