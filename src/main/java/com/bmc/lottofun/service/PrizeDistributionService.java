package com.bmc.lottofun.service;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.repository.UserRepository;
import com.bmc.lottofun.service.interfaces.IPrizeDistributionService;
import com.bmc.lottofun.service.interfaces.IUserService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrizeDistributionService implements IPrizeDistributionService {

    private final TicketRepository ticketRepository;
    private final IUserService userService;

    @Value("${lotto.prize.jackpot:100000.00}")
    private BigDecimal jackpotPrize;

    @Value("${lotto.prize.high:5000.00}")
    private BigDecimal highPrize;

    @Value("${lotto.prize.medium:250.00}")
    private BigDecimal mediumPrize;

    @Value("${lotto.ticket.number-count:5}")
    private int requiredNumberCount;

    private Map<Integer, PrizeCategory> prizeCategoryMap;

    @PostConstruct
    public void initPrizeCategories() {
        prizeCategoryMap = Map.of(
                requiredNumberCount, new PrizeCategory(TicketStatus.JACKPOT, jackpotPrize),
                requiredNumberCount - 1, new PrizeCategory(TicketStatus.HIGH, highPrize),
                requiredNumberCount - 2, new PrizeCategory(TicketStatus.MEDIUM, mediumPrize)
        );
    }

    @Transactional
    public void distributePrizes(Draw draw, Set<Integer> winningNumbers) {
        List<Ticket> tickets = ticketRepository.findByDraw(draw);

        for (Ticket ticket : tickets) {
            int matchCount = (int) ticket.getNumbers().stream()
                    .filter(winningNumbers::contains)
                    .count();

            PrizeCategory category = prizeCategoryMap.getOrDefault(
                    matchCount, new PrizeCategory(TicketStatus.NO_PRIZE, BigDecimal.ZERO));

            ticket.setMatchCount(matchCount);
            ticket.setStatus(category.getStatus());
            ticket.setPrizeAmount(category.getPrize());

            if (category.getPrize().compareTo(BigDecimal.ZERO) > 0) {
                userService.addBalance(ticket.getUser(), category.getPrize());
                log.info("User Id: {} won. Status: {}, Prize: {} $",
                        ticket.getUser().getId(), category.getStatus(), category.getPrize());
            }
        }
        ticketRepository.saveAll(tickets);
    }

    @AllArgsConstructor
    @Getter
    private static class PrizeCategory {
        private final TicketStatus status;
        private final BigDecimal prize;
    }
}
