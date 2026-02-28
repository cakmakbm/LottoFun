package com.bmc.lottofun.controller;

import com.bmc.lottofun.dto.request.TicketPurchaseRequest;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;


    @PostMapping("/buy")
    public ResponseEntity<String> buyTicket(@Valid @RequestBody TicketPurchaseRequest request) {

        ticketService.buyTickets(request);
        return new ResponseEntity<>("New ticket created!",HttpStatus.CREATED);

    }
}
