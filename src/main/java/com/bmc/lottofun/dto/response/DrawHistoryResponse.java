package com.bmc.lottofun.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class DrawHistoryResponse {
    private Long drawId;
    private LocalDateTime date;
    private Set<Integer> winningNumbers;
    private Map<Integer, Long> winnersPerCategory;
}