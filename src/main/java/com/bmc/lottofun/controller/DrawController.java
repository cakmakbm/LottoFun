package com.bmc.lottofun.controller;

import com.bmc.lottofun.dto.response.DrawHistoryResponse;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.service.DrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/draws")
@RequiredArgsConstructor
public class DrawController {

    private final DrawService drawService;


    @PostMapping("/start")
    public ResponseEntity<Draw> startDraw() {
        Draw newDraw = drawService.startNewDraw();
        return new ResponseEntity<>(newDraw, HttpStatus.CREATED);
    }


    @PostMapping("/execute")
    public ResponseEntity<String> executeDraw() {

        Draw activeDraw = drawService.getActiveDraw();



        drawService.executeDraw(activeDraw);


        return ResponseEntity.ok("Çekiliş motoru arka planda çalıştırıldı! Numaralar çekiliyor ve ikramiyeler dağıtılıyor...");
    }
    @GetMapping("/history")
    public ResponseEntity<List<DrawHistoryResponse>> getDrawHistory() {
        return ResponseEntity.ok(drawService.getDrawHistory());
    }
}