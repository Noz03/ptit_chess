package com.ptit.chess.dto;

import lombok.Data;

@Data
public class SendInvitationRequest {
    private Long receiverId;
    private Long roomId;
}
