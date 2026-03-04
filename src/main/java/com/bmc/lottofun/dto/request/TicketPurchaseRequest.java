package com.bmc.lottofun.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;


@Getter
@Setter
public class TicketPurchaseRequest {

    @NotNull(message ="Selected Numbers can not be null")
    private List<Integer> selectedNumbers;


}
