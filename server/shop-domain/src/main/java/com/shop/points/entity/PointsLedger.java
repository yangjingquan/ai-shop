package com.shop.points.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("points_ledger")
public class PointsLedger extends BaseEntity { private Long userId; private Long merchantId; private Integer changeValue; private Integer balanceAfter; private String source; private String businessNo; private Long relatedLedgerId; private String description; private LocalDateTime expireAt; }
