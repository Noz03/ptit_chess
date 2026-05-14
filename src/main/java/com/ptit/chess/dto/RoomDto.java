package com.ptit.chess.dto;

import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.entity.RoomType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoomDto {
    private Long id;
    private String roomCode;
    private Long hostPlayerId;
    private String hostDisplayName;
    private Long guestPlayerId;
    private String guestDisplayName;
    private RoomType roomType;
    private RoomStatus status;
    private LocalDateTime createdAt;
}
