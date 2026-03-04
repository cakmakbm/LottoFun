package com.bmc.lottofun.repository;
import com.bmc.lottofun.entity.Draw;
import com.bmc.lottofun.entity.Ticket;
import com.bmc.lottofun.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByDraw(Draw draw);
    List<Ticket> findByDrawIdAndMatchCountGreaterThanEqual(Long drawId, Integer matchCount);
    List<Ticket> findByUserOrderByPurchaseTimeDesc(User user);
}
