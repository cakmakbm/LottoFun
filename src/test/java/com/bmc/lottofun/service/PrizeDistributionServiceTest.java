package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.service.interfaces.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrizeDistributionServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private IUserService userService;

    @InjectMocks
    private PrizeDistributionService prizeDistributionService;

    private Draw draw;
    private User user;
    private final Set<Integer> winningNumbers = Set.of(1, 2, 3, 4, 5);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(prizeDistributionService, "jackpotPrize", new BigDecimal("100000.00"));
        ReflectionTestUtils.setField(prizeDistributionService, "highPrize", new BigDecimal("5000.00"));
        ReflectionTestUtils.setField(prizeDistributionService, "mediumPrize", new BigDecimal("250.00"));
        ReflectionTestUtils.setField(prizeDistributionService, "requiredNumberCount", 5);
        // Manually trigger @PostConstruct since Spring context is not loaded
        prizeDistributionService.initPrizeCategories();

        user = User.builder().id(1L).username("testuser").balance(BigDecimal.ZERO).build();
        draw = Draw.builder().id(1L).status(DrawStatus.COMPLETED).date(LocalDateTime.now()).build();
    }

    private Ticket buildTicket(Set<Integer> numbers) {
        return Ticket.builder()
                .user(user)
                .draw(draw)
                .numbers(numbers)
                .status(TicketStatus.PENDING)
                .prizeAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("5 matches should award JACKPOT prize and update user balance")
    void distributePrizes_allMatch_shouldSetJackpot() {
        Ticket ticket = buildTicket(Set.of(1, 2, 3, 4, 5));
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of(ticket));

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.JACKPOT);
        assertThat(ticket.getPrizeAmount()).isEqualByComparingTo("100000.00");
        assertThat(ticket.getMatchCount()).isEqualTo(5);
        verify(userService).addBalance(user, new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("4 matches should award HIGH prize")
    void distributePrizes_fourMatch_shouldSetHigh() {
        Ticket ticket = buildTicket(Set.of(1, 2, 3, 4, 99));
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of(ticket));

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HIGH);
        assertThat(ticket.getPrizeAmount()).isEqualByComparingTo("5000.00");
        assertThat(ticket.getMatchCount()).isEqualTo(4);
        verify(userService).addBalance(user, new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("3 matches should award MEDIUM prize")
    void distributePrizes_threeMatch_shouldSetMedium() {
        Ticket ticket = buildTicket(Set.of(1, 2, 3, 88, 99));
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of(ticket));

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.MEDIUM);
        assertThat(ticket.getPrizeAmount()).isEqualByComparingTo("250.00");
        verify(userService).addBalance(user, new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("2 or fewer matches should set NO_PRIZE and not update balance")
    void distributePrizes_twoOrLessMatch_shouldSetNoPrize() {
        Ticket ticket = buildTicket(Set.of(1, 2, 77, 88, 99));
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of(ticket));

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NO_PRIZE);
        assertThat(ticket.getPrizeAmount()).isEqualByComparingTo("0.00");
        verify(userService, never()).addBalance(any(), any());
    }

    @Test
    @DisplayName("No tickets in draw should result in no operations")
    void distributePrizes_noTickets_shouldDoNothing() {
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of());

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        verify(userService, never()).addBalance(any(), any());
        verify(ticketRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("Multiple tickets should each be evaluated in their correct prize category")
    void distributePrizes_multipleTickets_eachGetsCorrectCategory() {
        Ticket jackpotTicket = buildTicket(Set.of(1, 2, 3, 4, 5));
        Ticket noPrizeTicket = buildTicket(Set.of(10, 20, 30, 40, 49));
        when(ticketRepository.findByDraw(draw)).thenReturn(List.of(jackpotTicket, noPrizeTicket));

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        assertThat(jackpotTicket.getStatus()).isEqualTo(TicketStatus.JACKPOT);
        assertThat(noPrizeTicket.getStatus()).isEqualTo(TicketStatus.NO_PRIZE);
        verify(userService, times(1)).addBalance(any(), any());
    }
}
