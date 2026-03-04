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

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawExecutionService implements IDrawExecutionService {

    private final DrawRepository drawRepository;
    private final INumberGeneratorService numberGeneratorService;
    private final IPrizeDistributionService prizeDistributionService;

    @Async
    @Transactional
    public void executeDraw(Draw draw) {
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
    }
}
