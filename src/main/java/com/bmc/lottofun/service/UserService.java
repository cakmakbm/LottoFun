package com.bmc.lottofun.service;


import com.bmc.lottofun.dto.request.UserLoginRequest;
import com.bmc.lottofun.dto.request.UserRegisterRequest;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class UserService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");
    private final UserRepository userRepository;

    @Transactional
    public User registerUser(UserRegisterRequest request) {

        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("User name already exists");

        }
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .balance(INITIAL_BALANCE)
                .build();
        return userRepository.save(user);

    }

    public User loginUser(UserLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(!user.getPassword().equals(request.getPassword())){
            throw new RuntimeException("Incorrect password");
        }
        return user;
    }
}
