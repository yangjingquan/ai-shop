package com.shop.points.dto;

import lombok.Data;

@Data
public class MemberLevelVO {
    private Long id;
    private Integer level;
    private String name;
    private Integer minTotalPoints;
    private Integer maxTotalPoints;
}
