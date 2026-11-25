package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.exception.BadRequestException;
import com.bmc.lottofun.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .balance(new BigDecimal("100.00"))
                .build();
    }

    // --- deductBalance ---

    @Test
    @DisplayName("Sufficient balance should be deducted with a single atomic update")
    void deductBalance_sufficientFunds_shouldUseAtomicUpdate() {
        BigDecimal ticketPrice = new BigDecimal("10.00");
        when(userRepository.deductBalanceIfSufficient(1L, ticketPrice)).thenReturn(1);

        assertThatCode(() -> userService.deductBalance(user, ticketPrice)).doesNotThrowAnyException();

        verify(userRepository).deductBalanceIfSufficient(1L, ticketPrice);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Insufficient balance (no row updated) should throw BadRequestException")
    void deductBalance_insufficientFunds_shouldThrow() {
        BigDecimal amount = new BigDecimal("200.00");
        when(userRepository.deductBalanceIfSufficient(1L, amount)).thenReturn(0);

        assertThatThrownBy(() -> userService.deductBalance(user, amount))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("Zero or negative amount should be rejected before touching the database")
    void deductBalance_nonPositiveAmount_shouldThrow() {
        assertThatThrownBy(() -> userService.deductBalance(user, new BigDecimal("-10.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Amount must be positive");
        assertThatThrownBy(() -> userService.deductBalance(user, BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).deductBalanceIfSufficient(anyLong(), any());
    }

    // --- addBalance ---

    @Test
    @DisplayName("Prize amount should be added with a single atomic update")
    void addBalance_shouldUseAtomicUpdate() {
        BigDecimal prize = new BigDecimal("5000.00");
        when(userRepository.addBalance(1L, prize)).thenReturn(1);

        userService.addBalance(user, prize);

        verify(userRepository).addBalance(1L, prize);
        verify(userRepository, never()).save(any());
    }
}
