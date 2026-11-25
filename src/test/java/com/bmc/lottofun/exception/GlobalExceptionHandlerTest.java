package com.bmc.lottofun.exception;

import com.bmc.lottofun.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Unexpected errors return 500 without leaking the internal message")
    void unexpectedError_shouldHideInternalMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("SQL error: relation users does not exist"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred")
                .doesNotContain("SQL");
    }

    @Test
    @DisplayName("Wrong credentials return 401 with a generic message")
    void badCredentials_shouldReturn401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthentication(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("Missing role returns 403 instead of 500")
    void accessDenied_shouldReturn403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
