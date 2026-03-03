package com.bmc.lottofun.service;
import com.bmc.lottofun.dto.request.TicketPurchaseRequest;
import com.bmc.lottofun.dto.response.TicketResultResponse;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.*;





@Service
@RequiredArgsConstructor
public class TicketService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DrawRepository drawRepository;


    @Value("${lottery.rule.number-count}")
    private int requiredNumberCount;

    @Value("${lottery.rule.min-number}")
    private int minNumber;

    @Value("${lottery.rule.max-number}")
    private int maxNumber;

    @Value("${lottery.rule.ticket-price}")
    private BigDecimal ticketPrice;

    public void buyTickets(@NotNull TicketPurchaseRequest ticketPurchaseRequest) {
        List<Integer> selectedNumbers = ticketPurchaseRequest.getSelectedNumbers();

        if(selectedNumbers.size() != requiredNumberCount) {
            throw new RuntimeException("Number of selected numbers must be " + requiredNumberCount + " but found " + selectedNumbers.size());

        }
        Set<Integer> selectedNumbersSet = new HashSet<>(selectedNumbers);
        if(selectedNumbersSet.size() != selectedNumbers.size()) {
            throw new RuntimeException("Numbers must be unique");
        }


        for(Integer number : selectedNumbers) {

            if(number < minNumber) {
                throw new RuntimeException("Selected number "+ number + " is less than minimum number " + minNumber);
            }
            if(number > maxNumber) {
                throw new RuntimeException("Selected number "+ number + " is greater than maximum number " + maxNumber);
            }
        }


        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Giriş yapan kullanıcı bulunamadı!"));

        if(user.getBalance().compareTo(ticketPrice) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        Draw draw = drawRepository.findByStatus(DrawStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No active draw"));

        user.setBalance(user.getBalance().subtract(ticketPrice));
        userRepository.save(user);

        Ticket ticket = Ticket.builder()
                .user(user)
                .draw(draw)
                .numbers(selectedNumbersSet)
                .status(TicketStatus.PENDING)
                .purchaseTime(LocalDateTime.now())
                .prizeAmount(BigDecimal.ZERO)
                .build();
        ticketRepository.save(ticket);
    }

    public Map<Integer, Long> getWinnerCountsByDraw(Long drawId) {
        return ticketRepository.findByDrawIdAndMatchCountGreaterThanEqual(drawId, 3)
                .stream()
                .collect(Collectors.groupingBy(Ticket::getMatchCount, Collectors.counting()));
    }


    public List<TicketResultResponse> getMyTickets() {


        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();


        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Giriş yapan kullanıcı bulunamadı!"));


        List<Ticket> myTickets = ticketRepository.findByUserOrderByPurchaseTimeDesc(user);


        return myTickets.stream().map(ticket -> {
            Draw draw = ticket.getDraw();

            return TicketResultResponse.builder()
                    .ticketId(ticket.getId())
                    .drawId(draw.getId())
                    .purchaseTime(ticket.getPurchaseTime())
                    .selectedNumbers(ticket.getNumbers())
                    .drawStatus(draw.getStatus().name())
                    .winningNumbers(draw.getStatus() == DrawStatus.COMPLETED ? draw.getWinningNumbers() : null)
                    .matchCount(ticket.getMatchCount())
                    .prize(ticket.getPrizeAmount())
                    .ticketStatus(ticket.getStatus().name())
                    .build();

        }).collect(Collectors.toList());
    }

}
