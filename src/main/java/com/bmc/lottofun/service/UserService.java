package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.exception.BadRequestException;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.UserRepository;
import com.bmc.lottofun.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;

    @Transactional
    public void deductBalance(User user, BigDecimal amount) {
        requirePositive(amount);
        int updated = userRepository.deductBalanceIfSufficient(user.getId(), amount);
        if (updated == 0) {
            throw new BadRequestException("Insufficient balance");
        }
    }

    @Transactional
    public void addBalance(User user, BigDecimal amount) {
        requirePositive(amount);
        int updated = userRepository.addBalance(user.getId(), amount);
        if (updated == 0) {
            throw new NotFoundException("User not found");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
    }
}
