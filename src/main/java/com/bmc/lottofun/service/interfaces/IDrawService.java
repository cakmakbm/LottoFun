package com.bmc.lottofun.service.interfaces;

import com.bmc.lottofun.dto.response.DrawHistoryResponse;
import com.bmc.lottofun.entity.Draw;

import java.util.List;

public interface IDrawService {
    Draw startNewDraw();
    Draw getActiveDraw();
    List<DrawHistoryResponse> getDrawHistory();
}
