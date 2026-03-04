package com.bmc.lottofun.service;

import com.bmc.lottofun.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TicketValidatorTest {

    private TicketValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketValidator();
        ReflectionTestUtils.setField(validator, "requiredNumberCount", 5);
        ReflectionTestUtils.setField(validator, "minNumber", 1);
        ReflectionTestUtils.setField(validator, "maxNumber", 49);
    }

    @Test
    @DisplayName("Valid number list should pass validation without exception")
    void validate_validNumbers_shouldNotThrow() {
        List<Integer> numbers = List.of(1, 10, 20, 30, 49);
        assertThatCode(() -> validator.validate(numbers)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Fewer numbers than required should throw BadRequestException")
    void validate_wrongCount_shouldThrow() {
        List<Integer> numbers = List.of(1, 2, 3);
        assertThatThrownBy(() -> validator.validate(numbers))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("Duplicate numbers should throw BadRequestException")
    void validate_duplicateNumbers_shouldThrow() {
        List<Integer> numbers = List.of(1, 1, 2, 3, 4);
        assertThatThrownBy(() -> validator.validate(numbers))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unique");
    }

    @Test
    @DisplayName("Number below minimum should throw BadRequestException")
    void validate_numberBelowMin_shouldThrow() {
        List<Integer> numbers = List.of(0, 2, 3, 4, 5);
        assertThatThrownBy(() -> validator.validate(numbers))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("range");
    }

    @Test
    @DisplayName("Number above maximum should throw BadRequestException")
    void validate_numberAboveMax_shouldThrow() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 50);
        assertThatThrownBy(() -> validator.validate(numbers))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("range");
    }

    @Test
    @DisplayName("Boundary values (1 and 49) should be valid")
    void validate_boundaryNumbers_shouldNotThrow() {
        List<Integer> numbers = List.of(1, 49, 25, 13, 37);
        assertThatCode(() -> validator.validate(numbers)).doesNotThrowAnyException();
    }
}
