package com.shop.points.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data @EqualsAndHashCode(callSuper = true) @TableName("member_profile")
public class MemberProfile extends BaseEntity {
    private Long userId;
    private Long merchantId;
    private Integer status;
    private LocalDateTime joinedAt;
}
