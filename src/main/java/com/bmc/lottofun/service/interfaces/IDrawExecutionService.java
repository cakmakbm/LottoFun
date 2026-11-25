package com.bmc.lottofun.service.interfaces;

import com.bmc.lottofun.entity.Draw;

import java.util.concurrent.CompletableFuture;

public interface IDrawExecutionService {

    /**
     * Executes the draw asynchronously.
     *
     * @return completes with {@code true} if this call executed the draw, or {@code false}
     *         if another execution had already claimed it
     */
    CompletableFuture<Boolean> executeDraw(Draw draw);
}
