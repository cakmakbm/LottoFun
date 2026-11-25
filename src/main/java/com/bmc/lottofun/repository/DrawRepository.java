package com.bmc.lottofun.repository;

import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.enums.DrawStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrawRepository extends JpaRepository<Draw, Long> {

    Optional<Draw> findByStatus(DrawStatus status);

    List<Draw> findByStatusOrderByDateDesc(DrawStatus status);

    /**
     * Atomically moves a draw from {@code open} to {@code completed}.
     *
     * @return 1 if this call claimed the draw, 0 if it was no longer open
     */
    @Modifying
    @Query("UPDATE Draw d SET d.status = :completed WHERE d.id = :id AND d.status = :open")
    int markCompletedIfOpen(@Param("id") Long id,
                            @Param("open") DrawStatus open,
                            @Param("completed") DrawStatus completed);
}
