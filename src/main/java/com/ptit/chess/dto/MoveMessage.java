package com.ptit.chess.dto;

import lombok.Data;

@Data
public class MoveMessage {
    private String san;
    private String pgn;
    private String fen;
}
