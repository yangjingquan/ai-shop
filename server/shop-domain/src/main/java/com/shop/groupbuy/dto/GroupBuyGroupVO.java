package com.shop.groupbuy.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GroupBuyGroupVO {
    private Long id;
    private Long productId;
    private Integer requiredCount;
    private Integer paidCount;
    private Integer status;
    private String statusText;
    private Long expireAt;
    private Long formedAt;
    private Integer remainingCount;
    private String leaderNickname;
    private String leaderAvatar;
    private List<GroupBuyMemberVO> members = new ArrayList<>();
}
