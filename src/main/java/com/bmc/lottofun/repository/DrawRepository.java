package com.bmc.lottofun.repository;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrawRepository extends JpaRepository<Draw, Long> {

    Optional<Draw> findByStatus(DrawStatus status);

    List<Draw> findByStatusOrderByDateDesc(DrawStatus status);
}
