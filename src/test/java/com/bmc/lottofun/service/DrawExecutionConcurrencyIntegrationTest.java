package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.service.interfaces.IDrawExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Triggers the same draw many times at once through the real @Async executor,
 * as happens when an admin double-clicks "execute" or the scheduler fires at the
 * same moment, and checks that the draw runs exactly once.
 */
@SpringBootTest
class DrawExecutionConcurrencyIntegrationTest {

    private static final int TRIGGERS = 10;

    @Autowired
    private IDrawExecutionService drawExecutionService;

    @Autowired
    private DrawRepository drawRepository;

    @AfterEach
    void cleanUp() {
        drawRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent triggers execute a draw exactly once and create exactly one next draw")
    void concurrentTriggers_shouldExecuteDrawOnce() throws Exception {
        // Date in the future so the 60-second scheduler does not pick this draw up during the test.
        Draw draw = drawRepository.save(Draw.builder()
                .date(LocalDateTime.now().plusDays(1))
                .status(DrawStatus.OPEN)
                .build());

        List<CompletableFuture<Boolean>> executions = new ArrayList<>();
        for (int i = 0; i < TRIGGERS; i++) {
            // Each trigger gets its own copy of the draw, like separate requests would.
            Draw copy = drawRepository.findById(draw.getId()).orElseThrow();
            executions.add(drawExecutionService.executeDraw(copy));
        }
        CompletableFuture.allOf(executions.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        long executed = executions.stream().filter(CompletableFuture::join).count();
        List<Draw> openDraws = drawRepository.findAll().stream()
                .filter(d -> d.getStatus() == DrawStatus.OPEN)
                .toList();

        assertThat(executed).isEqualTo(1);
        assertThat(drawRepository.findById(draw.getId()).orElseThrow().getStatus()).isEqualTo(DrawStatus.COMPLETED);
        assertThat(openDraws).hasSize(1);
    }
}
