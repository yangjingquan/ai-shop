package com.shop.seckill.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeckillActivityVO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime preheatAt;
    private Integer status;
    private String statusText;
    private List<SeckillAdminSessionVO> sessions = new ArrayList<>();
}
