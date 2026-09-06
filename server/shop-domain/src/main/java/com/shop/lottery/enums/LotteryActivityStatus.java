package com.shop.lottery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LotteryActivityStatus {
    DRAFT(0, "草稿"), ACTIVE(1, "已发布"), PAUSED(2, "已暂停"), ENDED(3, "已结束");
    private final int code;
    private final String text;
}
