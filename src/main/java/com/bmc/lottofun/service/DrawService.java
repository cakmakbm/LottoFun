package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.repository.DrawRepository;
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
public class DrawService {

    private final DrawRepository drawRepository;


    private final NumberGeneratorService numberGeneratorService;
    private final PrizeDistributionService prizeDistributionService;

    @Transactional
    public Draw startNewDraw() {
        if (drawRepository.findByStatus(DrawStatus.OPEN).isPresent()) {
            throw new RuntimeException("Already OPEN lottery exists");
        }

        Draw newDraw = Draw.builder()
                .date(LocalDateTime.now().plusDays(7))
                .status(DrawStatus.OPEN)
                .build();

        return drawRepository.save(newDraw);
    }


    @Async
    public void executeDraw(Draw currentDraw) {
        log.info("Lottery (Draw ID: {}) async start", currentDraw.getId());

        try {
            // 1. USTAYI ÇAĞIR: Kazanan numaraları üret
            Set<Integer> winningNumbers = numberGeneratorService.generateWinningNumbers();
            log.info("Lottery Done! Winning numbers: {}", winningNumbers);


            currentDraw.setWinningNumbers(winningNumbers);
            currentDraw.setStatus(DrawStatus.COMPLETED);
            drawRepository.save(currentDraw);


            log.info("Prize calculations");
            prizeDistributionService.distributePrizes(currentDraw, winningNumbers);


            Draw nextDraw = Draw.builder()
                    .date(LocalDateTime.now().plusDays(7))
                    .status(DrawStatus.OPEN)
                    .build();
            drawRepository.save(nextDraw);

            log.info("New lottery created!");

        } catch (Exception e) {
            log.error("Problem occurs when creating new lottery: {}", e.getMessage());
        }
    }

    public Draw getActiveDraw() {
        return drawRepository.findByStatus(DrawStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("Not OPEN lottery exists"));
    }
}