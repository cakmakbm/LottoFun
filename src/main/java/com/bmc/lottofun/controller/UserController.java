package com.bmc.lottofun.controller;

import com.bmc.lottofun.dto.request.UserRegisterRequest;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody UserRegisterRequest request) {

        User savedUser = userService.registerUser(request);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }



}
