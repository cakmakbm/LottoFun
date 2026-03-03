package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.dto.response.DrawHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final TicketService ticketService;


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


    public List<DrawHistoryResponse> getDrawHistory() {
        List<Draw> completedDraws = drawRepository.findByStatusOrderByDateDesc(DrawStatus.COMPLETED);

        return completedDraws.stream().map(draw -> DrawHistoryResponse.builder()
                .drawId(draw.getId())
                .date(draw.getDate())
                .winningNumbers(draw.getWinningNumbers())
                .winnersPerCategory(ticketService.getWinnerCountsByDraw(draw.getId()))
                .build()
        ).collect(Collectors.toList());
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void checkAndExecuteExpiredDraws() {

        drawRepository.findByStatus(DrawStatus.OPEN).ifPresent(draw -> {


            log.info("Current time = {}, Scheduled draw time = {}",
                    LocalDateTime.now(), draw.getDate());

            if (LocalDateTime.now().isAfter(draw.getDate())) {
                log.info("Time is up! The draw is starting automatically. Draw ID: {}", draw.getId());
                executeDraw(draw);
            }
        });
    }
}