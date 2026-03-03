package com.bmc.lottofun.service;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrizeDistributionService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Value("${lotto.prize.jackpot:100000.00}")
    private BigDecimal jackpotPrize;

    @Value("${lotto.prize.high:5000.00}")
    private BigDecimal highPrize;

    @Value("${lotto.prize.medium:250.00}")
    private BigDecimal mediumPrize;

    @Value("${lotto.ticket.number-count:5}")
    private int requiredNumberCount;

    @Transactional
    public void distributePrizes(Draw draw, Set<Integer> winningNumbers) {
        List<Ticket> tickets = ticketRepository.findByDraw(draw);

        for (Ticket ticket : tickets) {
            long matchCount = ticket.getNumbers().stream()
                    .filter(winningNumbers::contains)
                    .count();

            TicketStatus newStatus;
            BigDecimal prize;

            if (matchCount == requiredNumberCount) {
                newStatus = TicketStatus.JACKPOT;
                prize = jackpotPrize;
            } else if (matchCount == requiredNumberCount - 1) {
                newStatus = TicketStatus.HIGH;
                prize = highPrize;
            } else if (matchCount == requiredNumberCount - 2) {
                newStatus = TicketStatus.MEDIUM;
                prize = mediumPrize;
            } else {
                newStatus = TicketStatus.NO_PRIZE;
                prize = BigDecimal.ZERO;
            }
            ticket.setMatchCount((int) matchCount);
            ticket.setStatus(newStatus);
            ticket.setPrizeAmount(prize);


            if (prize.compareTo(BigDecimal.ZERO) > 0) {
                User user = ticket.getUser();
                user.setBalance(user.getBalance().add(prize));
                userRepository.save(user);
                log.info("User Id: {} won. Status: {}, Prize: {} $",
                        user.getId(), newStatus, prize);
            }
        }
        ticketRepository.saveAll(tickets);
    }
}
