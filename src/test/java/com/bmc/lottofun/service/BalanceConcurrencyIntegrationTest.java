package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.exception.BadRequestException;
import com.bmc.lottofun.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many purchases at the same account at the same moment, against a real
 * (H2) database, and checks that the account is never overdrawn and no
 * deduction is lost.
 */
@SpringBootTest
class BalanceConcurrencyIntegrationTest {

    private static final int THREADS = 20;
    private static final BigDecimal PRICE = new BigDecimal("10.00");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.findByUsername("concurrent-buyer").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Concurrent deductions never overdraw the balance or lose an update")
    void concurrentDeductions_shouldNeverOverdraw() throws Exception {
        // Balance covers exactly 5 tickets, but 20 requests arrive at once.
        User user = userRepository.save(User.builder()
                .username("concurrent-buyer")
                .password("not-used")
                .role("ROLE_USER")
                .balance(new BigDecimal("50.00"))
                .build());

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    userService.deductBalance(user, PRICE);
                    return true;
                } catch (BadRequestException insufficient) {
                    return false;
                }
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }

        BigDecimal finalBalance = userRepository.findById(user.getId()).orElseThrow().getBalance();
        assertThat(succeeded).isEqualTo(5);
        assertThat(finalBalance).isEqualByComparingTo("0.00");
    }
}
