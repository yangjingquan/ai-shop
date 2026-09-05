package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("points_account")
public class PointsAccount extends BaseEntity { private Long userId; private Long merchantId; private Integer balance; private Integer version; }
