package com.bmc.lottofun.service;

import com.bmc.lottofun.dto.request.UserLoginRequest;
import com.bmc.lottofun.dto.request.UserRegisterRequest;
import com.bmc.lottofun.dto.response.AuthResponse;
import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.exception.ConflictException;
import com.bmc.lottofun.repository.UserRepository;
import com.bmc.lottofun.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "initialBalance", new BigDecimal("100.00"));
    }

    // --- register ---

    @Test
    @DisplayName("New user should be saved with encoded password and initial balance, then return token")
    void register_newUser_shouldSaveAndReturnToken() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(jwtService.generateToken("newuser")).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt.token.here");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getPassword()).isEqualTo("hashed_password");
        assertThat(savedUser.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Duplicate username should throw ConflictException and not save user")
    void register_duplicateUsername_shouldThrowConflict() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("pass");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    @DisplayName("Valid credentials should authenticate and return a JWT token")
    void login_validCredentials_shouldReturnToken() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = User.builder().id(1L).username("testuser").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("testuser")).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("testuser", "password123")
        );
    }

    @Test
    @DisplayName("Wrong password should throw BadCredentialsException and not generate token")
    void login_wrongPassword_shouldThrow() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
