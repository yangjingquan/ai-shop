package com.shop.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class DataVersionVO {
    private long dataVersion;
    private LocalDateTime serverTime;
    private List<String> invalidatedScopes;
}
