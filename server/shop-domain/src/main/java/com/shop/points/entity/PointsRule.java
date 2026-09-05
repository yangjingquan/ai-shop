package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("points_rule")
public class PointsRule extends BaseEntity { private Long merchantId; private Integer registerPoints; private Integer payAmountYuan; private Integer pointsPerYuan; private Integer signInPoints; private Integer validDays; private Integer deductionPerYuan; private Integer deductionMaxPoints; private Integer status; }
