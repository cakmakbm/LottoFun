package com.bmc.lottofun.entity;

import com.bmc.lottofun.entity.enums.TicketStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.Set;

public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "draw_id", nullable = false)
    private Draw draw;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ticket_numbers",joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name ="number")
    private Set<Integer> numbers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime purchaseTime;

    private BigDecimal prizeAmount;


}
