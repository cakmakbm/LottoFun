package com.bmc.lottofun.service.interfaces;

import com.bmc.lottofun.dto.request.TicketPurchaseRequest;
import com.bmc.lottofun.dto.response.TicketResultResponse;

import java.util.List;
import java.util.Map;

public interface ITicketService {
    void buyTickets(TicketPurchaseRequest request);
    List<TicketResultResponse> getMyTickets();
    Map<String, Long> getWinnerCountsByDraw(Long drawId);
}
