package com.bmc.lottofun.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class TicketResultResponse {
    private Long ticketId;
    private Set<Integer> selectedNumbers;
    private String drawStatus;
    private Set<Integer> winningNumbers;
    private Integer matchCount;
    private BigDecimal prize;
    private String ticketStatus;
    private Long drawId;
    private LocalDateTime purchaseTime;
}