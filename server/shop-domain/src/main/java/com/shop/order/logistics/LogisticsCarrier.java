package com.shop.order.logistics;

import java.util.Arrays;

/** 快递鸟承运商编码。名称匹配仅用于兼容历史上只录入物流公司名称的订单。 */
public enum LogisticsCarrier {
    SF("顺丰", "SF"),
    ZTO("中通", "ZTO"),
    YTO("圆通", "YTO"),
    STO("申通", "STO"),
    YD("韵达", "YD"),
    JD("京东", "JD"),
    EMS("EMS", "EMS"),
    YZPY("邮政", "YZPY"),
    DBL("德邦", "DBL"),
    JTSD("极兔", "JTSD"),
    HTKY("百世", "HTKY");

    private final String name;
    private final String code;

    LogisticsCarrier(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() { return name; }
    public String getCode() { return code; }

    public static LogisticsCarrier resolve(String code, String name) {
        if (code != null && !code.isBlank()) {
            return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code.trim())).findFirst().orElse(null);
        }
        if (name != null && !name.isBlank()) {
            String normalized = name.trim().toLowerCase();
            return Arrays.stream(values()).filter(v -> normalized.contains(v.name.toLowerCase())
                    || normalized.equals(v.code.toLowerCase())).findFirst().orElse(null);
        }
        return null;
    }
}
