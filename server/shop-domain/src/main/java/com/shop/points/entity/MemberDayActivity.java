package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("member_day_activity")
public class MemberDayActivity extends BaseEntity { private Long merchantId; private String name; private Integer dayOfMonth; private LocalTime startTime; private LocalTime endTime; private Integer doublePoints; private Long couponTemplateId; private Integer productScopeType; private String productScopeIdsJson; private Integer stackable; private Integer status; }
