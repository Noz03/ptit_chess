package com.ptit.chess.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chess_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long whitePlayerId;

    @Column(nullable = false)
    private Long blackPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.IN_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String pgn;

    @Column(nullable = false)
    private Integer timeControl = 600; // seconds

    @Enumerated(EnumType.STRING)
    private MatchResult result;

    @Enumerated(EnumType.STRING)
    private MatchEndReason endReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
    }
}
