package com.shop.marketing.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 营销活动能力目录。implemented 表示当前版本是否已经接入真实业务链路。
 */
@Getter
public enum MarketingActivityCode {
    NEW_USER_COUPON("NEW_USER_COUPON", "新人首单券", "新用户完成首单时享受优惠", true, "/pages/coupon/list"),
    SECKILL("SECKILL", "限时秒杀", "在限定时间内以特价购买商品", true, "/pages/activity/seckill/list"),
    GROUP_BUY("GROUP_BUY", "多人拼团", "邀请好友成团后享受拼团价", true, "/pages/group-buy/list"),
    FULL_REDUCTION("FULL_REDUCTION", "满减活动", "订单达到门槛后自动减免", false, "/pages/promotion/full-reduction"),
    REPURCHASE_COUPON("REPURCHASE_COUPON", "复购券", "支付成功后赠送短期复购优惠券", true, "/pages/coupon/list"),
    POINTS_MEMBER_DAY("POINTS_MEMBER_DAY", "积分会员日", "会员日享受积分或专属权益", true, "/pages/points/member-day"),
    REFERRAL("REFERRAL", "邀请有礼", "邀请新用户首单后获得优惠券奖励", true, "/pages/activity/referral/index"),
    BUNDLE("BUNDLE", "组合购", "搭配购买多件商品享受组合优惠", false, "/pages/promotion/bundle"),
    PRESALE("PRESALE", "预售", "提前支付定金锁定未来商品", false, "/pages/presale/list"),
    LOTTERY_BLIND_BOX("LOTTERY_BLIND_BOX", "抽奖/盲盒", "通过抽奖或盲盒机制提升互动和转化", false, "/pages/lottery/index");

    private final String code;
    private final String name;
    private final String description;
    private final boolean implemented;
    private final String frontendPath;

    MarketingActivityCode(String code, String name, String description, boolean implemented, String frontendPath) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.implemented = implemented;
        this.frontendPath = frontendPath;
    }

    public static MarketingActivityCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
