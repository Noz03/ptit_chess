package com.ptit.chess.dto;

import com.ptit.chess.entity.ActivityStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerProfileDto {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Integer elo;
    private Integer winCount;
    private Integer drawCount;
    private Integer lossCount;
    private ActivityStatus activityStatus;
}
