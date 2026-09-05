package com.shop.bundle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bundle_item")
public class BundleItem extends BaseEntity {
    private Long bundleActivityId;
    private Long merchantId;
    private Long productId;
    private Integer required;
    private Integer sort;
}
