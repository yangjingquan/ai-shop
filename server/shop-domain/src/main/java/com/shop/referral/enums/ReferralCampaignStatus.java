package com.shop.referral.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReferralCampaignStatus {
    DRAFT(0, "草稿"), ACTIVE(1, "进行中"), PAUSED(2, "已暂停"), ENDED(3, "已结束");

    private final int code;
    private final String text;
}
