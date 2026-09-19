package com.shop.engagement.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.shop.common.entity.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("user_product_history") public class UserProductHistory extends BaseEntity {private Long userId;private Long merchantId;private Long productId;private Integer viewCount;private LocalDateTime lastViewedAt;}
