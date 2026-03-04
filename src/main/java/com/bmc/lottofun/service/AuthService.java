package com.bmc.lottofun.service;

import com.bmc.lottofun.dto.response.AuthResponse;
import com.bmc.lottofun.dto.request.UserLoginRequest;
import com.bmc.lottofun.dto.request.UserRegisterRequest;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.exception.ConflictException;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.UserRepository;
import com.bmc.lottofun.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    @Value("${lotto.initial-balance:100.00}")
    private BigDecimal initialBalance;


    public AuthResponse register(UserRegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("This username is already in use");
        }


        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .balance(initialBalance)
                .role("ROLE_USER")
                .build();

        userRepository.save(user);


        String jwtToken = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder().token(jwtToken).build();
    }


    public AuthResponse login(UserLoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );


        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUsername()));


        String jwtToken = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder().token(jwtToken).build();
    }
}