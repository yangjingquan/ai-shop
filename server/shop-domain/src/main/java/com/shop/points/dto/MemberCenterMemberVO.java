package com.shop.points.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberCenterMemberVO {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer status;
    private Integer level;
    private String levelName;
    private Integer pointsBalance;
    private Integer totalPoints;
    private LocalDateTime joinedAt;
    private LocalDateTime lastLoginAt;
}
