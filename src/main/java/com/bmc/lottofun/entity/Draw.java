package com.bmc.lottofun.entity;


import com.bmc.lottofun.entity.enums.DrawStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "draws")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Draw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "draw_winning_numbers", joinColumns = @JoinColumn(name= "draw_id"))
    @Column(name = "number")
    private Set<Integer> winningNumbers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrawStatus status;





}
