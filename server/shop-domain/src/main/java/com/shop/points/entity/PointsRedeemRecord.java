package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("points_redeem_record")
public class PointsRedeemRecord extends BaseEntity { private String redeemNo; private Long userId; private Long merchantId; private Long pointsProductId; private String orderNo; private Long couponId; private Integer pointsCost; private Integer quantity; private Integer status; }
