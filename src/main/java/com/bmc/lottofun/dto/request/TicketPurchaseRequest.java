package com.bmc.lottofun.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;


@Getter
@Setter
public class TicketPurchaseRequest {

    @NotNull(message = "user id can not be null")
    private long userId;


    @NotNull(message ="Selected Numbers can not be null")
    private List<Integer> selectedNumbers;


}
