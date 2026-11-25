package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.service.interfaces.IDrawExecutionService;
import com.bmc.lottofun.service.interfaces.INumberGeneratorService;
import com.bmc.lottofun.service.interfaces.IPrizeDistributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawExecutionService implements IDrawExecutionService {

    private final DrawRepository drawRepository;
    private final INumberGeneratorService numberGeneratorService;
    private final IPrizeDistributionService prizeDistributionService;

    @Async
    @Transactional
    public CompletableFuture<Boolean> executeDraw(Draw draw) {
        // The scheduler and the admin endpoint can trigger the same draw at the same time.
        // Only one execution may win: the OPEN -> COMPLETED switch is a single conditional
        // UPDATE, so a second thread either waits for the row lock and then updates 0 rows,
        // or sees COMPLETED right away. If this transaction rolls back, the draw is OPEN again.
        int claimed = drawRepository.markCompletedIfOpen(draw.getId(), DrawStatus.OPEN, DrawStatus.COMPLETED);
        if (claimed == 0) {
            log.info("Draw ID: {} was already executed, skipping.", draw.getId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Draw ID: {} executing...", draw.getId());

        Set<Integer> winningNumbers = numberGeneratorService.generateWinningNumbers();
        draw.setWinningNumbers(winningNumbers);
        draw.setStatus(DrawStatus.COMPLETED);
        drawRepository.save(draw);

        prizeDistributionService.distributePrizes(draw, winningNumbers);

        Draw nextDraw = Draw.builder()
                .date(LocalDateTime.now().plusDays(7))
                .status(DrawStatus.OPEN)
                .build();
        drawRepository.save(nextDraw);

        log.info("Draw completed, next draw created.");
        return CompletableFuture.completedFuture(true);
    }
}
