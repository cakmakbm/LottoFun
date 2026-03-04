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
    @DisplayName("Sufficient balance should be deducted and user saved")
    void deductBalance_sufficientFunds_shouldDeductAndSave() {
        BigDecimal ticketPrice = new BigDecimal("10.00");

        userService.deductBalance(user, ticketPrice);

        assertThat(user.getBalance()).isEqualByComparingTo("90.00");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Insufficient balance should throw BadRequestException and not save")
    void deductBalance_insufficientFunds_shouldThrow() {
        BigDecimal amount = new BigDecimal("200.00");

        assertThatThrownBy(() -> userService.deductBalance(user, amount))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient balance");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Amount equal to balance should succeed and result in zero balance")
    void deductBalance_exactBalance_shouldSucceed() {
        BigDecimal amount = new BigDecimal("100.00");

        userService.deductBalance(user, amount);

        assertThat(user.getBalance()).isEqualByComparingTo("0.00");
        verify(userRepository).save(user);
    }

    // --- addBalance ---

    @Test
    @DisplayName("Prize amount should be added to balance and user saved")
    void addBalance_shouldIncreaseBalanceAndSave() {
        BigDecimal prize = new BigDecimal("5000.00");

        userService.addBalance(user, prize);

        assertThat(user.getBalance()).isEqualByComparingTo("5100.00");
        verify(userRepository).save(user);
    }
}
