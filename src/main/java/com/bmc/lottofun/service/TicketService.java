package com.bmc.lottofun.service;
import com.bmc.lottofun.dto.request.TicketPurchaseRequest;
import com.bmc.lottofun.dto.response.TicketResultResponse;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.security.AuthHelper;
import com.bmc.lottofun.service.interfaces.ITicketService;
import com.bmc.lottofun.service.interfaces.ITicketValidator;
import com.bmc.lottofun.service.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.*;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class TicketService implements ITicketService {

    private final TicketRepository ticketRepository;
    private final DrawRepository drawRepository;
    private final IUserService userService;
    private final ITicketValidator ticketValidator;
    private final AuthHelper authHelper;

    @Value("${lottery.rule.ticket-price}")
    private BigDecimal ticketPrice;
    @Value("${lottery.rule.number-count}")
    private int numberCount;

    @Transactional
    public void buyTickets(TicketPurchaseRequest request) {
        ticketValidator.validate(request.getSelectedNumbers());

        User user = authHelper.getCurrentUser();

        Draw draw = drawRepository.findByStatus(DrawStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("No active draw"));

        userService.deductBalance(user, ticketPrice);

        Ticket ticket = Ticket.builder()
                .user(user)
                .draw(draw)
                .numbers(new HashSet<>(request.getSelectedNumbers()))
                .status(TicketStatus.PENDING)
                .purchaseTime(LocalDateTime.now())
                .prizeAmount(BigDecimal.ZERO)
                .build();
        ticketRepository.save(ticket);
    }

    public List<TicketResultResponse> getMyTickets() {
        User user = authHelper.getCurrentUser();
        List<Ticket> tickets = ticketRepository.findByUserOrderByPurchaseTimeDesc(user);

        return tickets.stream()
                .map(this::toResultResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getWinnerCountsByDraw(Long drawId) {
        return ticketRepository.findByDrawIdAndMatchCountGreaterThanEqual(drawId, numberCount - 2)
                .stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getStatus().name(),
                        Collectors.counting()
                ));
    }

    private TicketResultResponse toResultResponse(Ticket ticket) {
        Draw draw = ticket.getDraw();
        boolean completed = draw.getStatus() == DrawStatus.COMPLETED;

        return TicketResultResponse.builder()
                .ticketId(ticket.getId())
                .drawId(draw.getId())
                .purchaseTime(ticket.getPurchaseTime())
                .selectedNumbers(ticket.getNumbers())
                .drawStatus(draw.getStatus().name())
                .winningNumbers(completed ? draw.getWinningNumbers() : null)
                .matchCount(completed ? ticket.getMatchCount() : null)
                .prize(ticket.getPrizeAmount() != null && ticket.getPrizeAmount().compareTo(BigDecimal.ZERO) > 0
                        ? ticket.getPrizeAmount()
                        : null)
                .ticketStatus(ticket.getStatus().name())
                .build();
    }
}
