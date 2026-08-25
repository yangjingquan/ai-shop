package com.shop.groupbuy.dto;

import lombok.Data;

@Data
public class GroupBuyMemberVO {
    private String nickname;
    private String avatar;
    private boolean leader;
    private Integer status;
    private String statusText;
}
