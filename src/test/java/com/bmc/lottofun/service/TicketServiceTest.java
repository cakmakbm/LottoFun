package com.bmc.lottofun.service;

import com.bmc.lottofun.dto.request.TicketPurchaseRequest;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.entity.enums.TicketStatus;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.repository.TicketRepository;
import com.bmc.lottofun.security.AuthHelper;
import com.bmc.lottofun.service.interfaces.ITicketValidator;
import com.bmc.lottofun.service.interfaces.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private DrawRepository drawRepository;
    @Mock private IUserService userService;
    @Mock private ITicketValidator ticketValidator;
    @Mock private AuthHelper authHelper;

    @InjectMocks
    private TicketService ticketService;

    private User user;
    private Draw openDraw;
    private TicketPurchaseRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketService, "ticketPrice", new BigDecimal("10.00"));

        user = User.builder()
                .id(1L)
                .username("testuser")
                .balance(new BigDecimal("100.00"))
                .build();

        openDraw = Draw.builder()
                .id(1L)
                .status(DrawStatus.OPEN)
                .date(LocalDateTime.now().plusDays(7))
                .build();

        request = new TicketPurchaseRequest();
        request.setSelectedNumbers(List.of(1, 10, 20, 30, 49));
    }

    @Test
    @DisplayName("Successful purchase should deduct balance and save ticket with correct fields")
    void buyTickets_success_shouldDeductBalanceAndSaveTicket() {
        when(authHelper.getCurrentUser()).thenReturn(user);
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.of(openDraw));

        ticketService.buyTickets(request);

        verify(ticketValidator).validate(request.getSelectedNumbers());
        verify(userService).deductBalance(user, new BigDecimal("10.00"));

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());

        Ticket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getUser()).isEqualTo(user);
        assertThat(savedTicket.getDraw()).isEqualTo(openDraw);
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.PENDING);
        assertThat(savedTicket.getPrizeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedTicket.getNumbers()).containsExactlyInAnyOrder(1, 10, 20, 30, 49);
    }

    @Test
    @DisplayName("No active draw should throw NotFoundException and not save ticket")
    void buyTickets_noActiveDraw_shouldThrowNotFoundException() {
        when(authHelper.getCurrentUser()).thenReturn(user);
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.buyTickets(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No active draw");

        verify(ticketRepository, never()).save(any());
        verify(userService, never()).deductBalance(any(), any());
    }

    @Test
    @DisplayName("Validation failure should abort early and not save ticket or fetch user")
    void buyTickets_invalidNumbers_shouldNotSaveTicket() {
        doThrow(new com.bmc.lottofun.exception.BadRequestException("Number count must be 5"))
                .when(ticketValidator).validate(any());

        assertThatThrownBy(() -> ticketService.buyTickets(request))
                .isInstanceOf(com.bmc.lottofun.exception.BadRequestException.class);

        verify(ticketRepository, never()).save(any());
        verify(authHelper, never()).getCurrentUser();
    }

    @Test
    @DisplayName("Balance should be deducted exactly once per ticket purchase")
    void buyTickets_deductBalance_calledExactlyOnce() {
        when(authHelper.getCurrentUser()).thenReturn(user);
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.of(openDraw));

        ticketService.buyTickets(request);

        verify(userService, times(1)).deductBalance(user, new BigDecimal("10.00"));
    }
}
