package com.ptit.chess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(unique = true, nullable = false)
    private String displayName;

    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer elo = 1200;

    @Column(nullable = false)
    @Builder.Default
    private Integer winCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer drawCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer lossCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ActivityStatus activityStatus = ActivityStatus.OFFLINE;
}
