package com.ptit.chess.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDto {
    private long totalAccounts;
    private long lockedAccounts;
    private long onlinePlayers;
    private long openRooms;
    private long totalMatches;
}
