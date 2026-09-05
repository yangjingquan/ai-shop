package com.shop.points.dto;
import lombok.Data;
import java.time.LocalTime;
@Data public class MemberDayActivityVO { private Long id; private String name; private Integer dayOfMonth; private LocalTime startTime; private LocalTime endTime; private Integer doublePoints; private Long couponTemplateId; private Integer productScopeType; private String productScopeIdsJson; private Integer stackable; private Integer status; private boolean active; private String statusText; }
