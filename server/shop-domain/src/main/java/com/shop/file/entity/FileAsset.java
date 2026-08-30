package com.shop.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_asset")
public class FileAsset extends BaseEntity {
    private String publicUrl;
    private String ownerType;
    private Long ownerId;
    private Long merchantId;
}
