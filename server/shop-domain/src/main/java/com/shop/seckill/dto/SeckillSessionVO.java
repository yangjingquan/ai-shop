package com.shop.seckill.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeckillSessionVO {
    private Long id;
    private Long activityId;
    private String activityName;
    private String activityDescription;
    private String name;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    private String statusText;
    private List<SeckillProductVO> products = new ArrayList<>();
}
