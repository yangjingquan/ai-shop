package com.shop.groupbuy.dto;

import java.math.BigDecimal;
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
    private String productName;
    private String productImage;
    private BigDecimal groupBuyPrice;
    private BigDecimal originalPrice;
    private Integer remainingCount;
    private String leaderNickname;
    private String leaderAvatar;
    private List<GroupBuyMemberVO> members = new ArrayList<>();
}
