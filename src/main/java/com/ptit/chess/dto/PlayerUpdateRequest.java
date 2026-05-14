package com.ptit.chess.dto;

import lombok.Data;

@Data
public class PlayerUpdateRequest {
    private String displayName;
    private String avatarUrl;
}
