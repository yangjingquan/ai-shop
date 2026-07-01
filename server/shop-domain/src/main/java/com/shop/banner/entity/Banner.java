package com.shop.banner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class Banner extends BaseEntity {

    private String imageUrl;

    /** 0=NONE 1=PRODUCT 2=CATEGORY 3=URL */
    private Integer linkType;

    private String linkValue;

    private Integer sort;

    /** 1=显示 0=隐藏 */
    private Integer status;
}
