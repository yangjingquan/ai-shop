package com.shop.presale.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PresaleActivityVO {
    private Long id;
    private String name;
    private String description;
    private String bannerImage;
    private LocalDateTime depositStartAt;
    private LocalDateTime depositEndAt;
    private LocalDateTime balanceStartAt;
    private LocalDateTime balanceEndAt;
    private LocalDateTime expectedShipAt;
    private Integer status;
    private String statusText;
    private Integer autoCloseExpired;
    private String depositRefundRule;
    private String merchantBreachRule;
    private Integer phase;
    private String phaseText;
    private List<PresaleSkuVO> skus = new ArrayList<>();
}
