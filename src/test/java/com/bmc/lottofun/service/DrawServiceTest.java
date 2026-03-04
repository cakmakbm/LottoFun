package com.bmc.lottofun.service;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import com.bmc.lottofun.exception.ConflictException;
import com.bmc.lottofun.exception.NotFoundException;
import com.bmc.lottofun.repository.DrawRepository;
import com.bmc.lottofun.service.interfaces.IDrawExecutionService;
import com.bmc.lottofun.service.interfaces.ITicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawServiceTest {

    @Mock private DrawRepository drawRepository;
    @Mock private ITicketService ticketService;
    @Mock private IDrawExecutionService drawExecutionService;

    @InjectMocks
    private DrawService drawService;

    // --- startNewDraw ---

    @Test
    @DisplayName("No existing open draw should create a new OPEN draw scheduled 7 days ahead")
    void startNewDraw_noExistingOpenDraw_shouldCreateAndReturn() {
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.empty());
        Draw saved = Draw.builder().id(1L).status(DrawStatus.OPEN).build();
        when(drawRepository.save(any(Draw.class))).thenReturn(saved);

        Draw result = drawService.startNewDraw();

        assertThat(result.getStatus()).isEqualTo(DrawStatus.OPEN);

        ArgumentCaptor<Draw> captor = ArgumentCaptor.forClass(Draw.class);
        verify(drawRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DrawStatus.OPEN);
        assertThat(captor.getValue().getDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Already open draw should throw ConflictException and not save")
    void startNewDraw_alreadyOpenDraw_shouldThrowConflict() {
        Draw existing = Draw.builder().id(1L).status(DrawStatus.OPEN).build();
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> drawService.startNewDraw())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("OPEN");

        verify(drawRepository, never()).save(any());
    }

    // --- getActiveDraw ---

    @Test
    @DisplayName("Existing open draw should be returned successfully")
    void getActiveDraw_openDrawExists_shouldReturn() {
        Draw open = Draw.builder().id(1L).status(DrawStatus.OPEN).build();
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.of(open));

        Draw result = drawService.getActiveDraw();

        assertThat(result).isEqualTo(open);
    }

    @Test
    @DisplayName("No open draw should throw NotFoundException")
    void getActiveDraw_noOpenDraw_shouldThrowNotFound() {
        when(drawRepository.findByStatus(DrawStatus.OPEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> drawService.getActiveDraw())
                .isInstanceOf(NotFoundException.class);
    }
}
