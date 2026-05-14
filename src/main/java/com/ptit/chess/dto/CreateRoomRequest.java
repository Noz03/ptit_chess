package com.ptit.chess.dto;

import com.ptit.chess.entity.RoomType;
import lombok.Data;

@Data
public class CreateRoomRequest {
    private RoomType roomType;
}
