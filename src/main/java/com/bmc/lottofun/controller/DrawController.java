package com.bmc.lottofun.controller;

import com.bmc.lottofun.dto.response.DrawHistoryResponse;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.service.interfaces.IDrawExecutionService;
import com.bmc.lottofun.service.interfaces.IDrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/draws")
@RequiredArgsConstructor
public class DrawController {

    private final IDrawService drawService;
    private final IDrawExecutionService drawExecutionService;


    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Draw> startDraw() {
        Draw newDraw = drawService.startNewDraw();
        return new ResponseEntity<>(newDraw, HttpStatus.CREATED);
    }


    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> executeDraw() {

        Draw activeDraw = drawService.getActiveDraw();



        drawExecutionService.executeDraw(activeDraw);


        return ResponseEntity.ok("Draw execution started. Results will be processed asynchronously.");
    }
    @GetMapping("/history")
    public ResponseEntity<List<DrawHistoryResponse>> getDrawHistory() {
        return ResponseEntity.ok(drawService.getDrawHistory());
    }
}