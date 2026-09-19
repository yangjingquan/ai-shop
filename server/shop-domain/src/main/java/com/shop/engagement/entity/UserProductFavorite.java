package com.shop.engagement.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.shop.common.entity.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("user_product_favorite") public class UserProductFavorite extends BaseEntity {private Long userId;private Long merchantId;private Long productId;}
