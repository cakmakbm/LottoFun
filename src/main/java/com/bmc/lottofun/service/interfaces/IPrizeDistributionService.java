package com.bmc.lottofun.service.interfaces;

import com.bmc.lottofun.entity.Draw;

import java.util.Set;

public interface IPrizeDistributionService {
    void distributePrizes(Draw draw, Set<Integer> winningNumbers);
}
