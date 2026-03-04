package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.exception.ConflictException;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.dto.response.DrawHistoryResponse;
import com.bmc.lottofun.service.interfaces.IDrawExecutionService;
import com.bmc.lottofun.service.interfaces.IDrawService;
import com.bmc.lottofun.service.interfaces.ITicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
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
public class DrawService implements IDrawService {

    private final DrawRepository drawRepository;
    private final ITicketService ticketService;
    private final IDrawExecutionService drawExecutionService;

    @Transactional
    public Draw startNewDraw() {
        if (drawRepository.findByStatus(DrawStatus.OPEN).isPresent()) {
            throw new ConflictException("Already OPEN lottery exists");
        }

        Draw newDraw = Draw.builder()
                .date(LocalDateTime.now().plusDays(7))
                .status(DrawStatus.OPEN)
                .build();

        return drawRepository.save(newDraw);
    }

    public Draw getActiveDraw() {
        return drawRepository.findByStatus(DrawStatus.OPEN)
                .orElseThrow(() -> new NotFoundException("No OPEN lottery exists"));
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

    @Scheduled(fixedRate = 60000)
    public void checkAndExecuteExpiredDraws() {
        drawRepository.findByStatus(DrawStatus.OPEN).ifPresent(draw -> {
            if (LocalDateTime.now().isAfter(draw.getDate())) {
                log.info("Time is up! Draw ID: {}", draw.getId());
                drawExecutionService.executeDraw(draw);
            }
        });
    }
}