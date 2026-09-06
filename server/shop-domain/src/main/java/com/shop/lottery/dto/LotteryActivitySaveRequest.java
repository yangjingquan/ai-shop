package com.shop.lottery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotteryActivitySaveRequest {
    /** 编辑接口兼容后台详情对象回传的活动 ID，实际以路径参数为准。 */
    private Long id;
    @NotBlank private String name;
    private String themeImage;
    private String entryImage;
    @NotBlank private String ruleText;
    @Valid @NotNull private LotteryCondition condition;
    @NotNull private Integer dailyChances;
    @NotNull private LocalDateTime startAt;
    @NotNull private LocalDateTime endAt;
    private Integer status = 0;
    @Valid @NotEmpty private List<LotteryPrizeRequest> prizes;
}
