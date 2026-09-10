package com.shop.home.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_module_config")
public class HomeModuleConfig extends BaseEntity {
    private Long merchantId;
    private String moduleCode;
    private String title;
    private String subtitle;
    private Integer enabled;
    private Integer sortOrder;
    private String productSource;
    private Integer productLimit;
}
