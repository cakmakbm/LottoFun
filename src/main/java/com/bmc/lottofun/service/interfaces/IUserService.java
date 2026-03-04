package com.bmc.lottofun.service.interfaces;

import com.bmc.lottofun.entity.User;

import java.math.BigDecimal;

public interface IUserService {
    void deductBalance(User user, BigDecimal amount);
    void addBalance(User user, BigDecimal amount);
}
