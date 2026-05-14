package com.ptit.chess.controller;

import com.ptit.chess.dto.MoveMessage;
import com.ptit.chess.entity.MatchEndReason;
import com.ptit.chess.entity.MatchResult;
import com.ptit.chess.entity.Player;
import com.ptit.chess.security.CustomUserDetails;
import com.ptit.chess.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class MatchWebSocketController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/match/{matchId}/move")
    public void makeMove(@DestinationVariable Long matchId, MoveMessage message, Authentication authentication) {
        if (authentication == null) return;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        matchService.handleMove(matchId, player.getId(), message.getSan(), message.getPgn());
    }

    @MessageMapping("/match/{matchId}/resign")
    public void resign(@DestinationVariable Long matchId, Authentication authentication) {
        if (authentication == null) return;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        // In a real app we need to check if player is white or black to determine the winner
        // For simplicity, we just pass the info to MatchService. We'll update MatchService to accept playerId for resignation.
        matchService.handleResign(matchId, player.getId());
    }

    @MessageMapping("/match/{matchId}/offer-draw")
    public void offerDraw(@DestinationVariable Long matchId, Authentication authentication) {
        if (authentication == null) return;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        messagingTemplate.convertAndSend("/topic/match/" + matchId, "DRAW_OFFERED:" + player.getId());
    }

    @MessageMapping("/match/{matchId}/accept-draw")
    public void acceptDraw(@DestinationVariable Long matchId, Authentication authentication) {
        if (authentication == null) return;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        matchService.endGame(matchId, MatchResult.DRAW, MatchEndReason.AGREED_DRAW);
    }
    
    @MessageMapping("/match/{matchId}/game-over")
    public void gameOver(@DestinationVariable Long matchId, String result, Authentication authentication) {
        if (authentication == null) return;
        
        // Client reports checkmate or draw
        if ("WHITE_WIN".equals(result)) {
            matchService.endGame(matchId, MatchResult.WHITE_WIN, MatchEndReason.CHECKMATE);
        } else if ("BLACK_WIN".equals(result)) {
            matchService.endGame(matchId, MatchResult.BLACK_WIN, MatchEndReason.CHECKMATE);
        } else if ("DRAW".equals(result)) {
            matchService.endGame(matchId, MatchResult.DRAW, MatchEndReason.AGREED_DRAW); // Could be stalemate
        }
    }
}
