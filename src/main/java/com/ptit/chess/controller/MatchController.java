package com.ptit.chess.controller;

import com.ptit.chess.entity.Match;
import com.ptit.chess.entity.MatchStatus;
import com.ptit.chess.entity.Player;
import com.ptit.chess.repository.MatchRepository;
import com.ptit.chess.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getActiveMatchByRoom(@PathVariable Long roomId) {
        Optional<Match> matchOpt = matchRepository.findByRoomIdAndStatus(roomId, MatchStatus.IN_PROGRESS);
        if (matchOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Match match = matchOpt.get();
        Player whitePlayer = playerRepository.findById(match.getWhitePlayerId()).orElse(null);
        Player blackPlayer = playerRepository.findById(match.getBlackPlayerId()).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", match.getId());
        response.put("roomId", match.getRoomId());
        response.put("whitePlayerId", match.getWhitePlayerId());
        response.put("whitePlayerName", whitePlayer != null ? whitePlayer.getDisplayName() : "Unknown");
        response.put("blackPlayerId", match.getBlackPlayerId());
        response.put("blackPlayerName", blackPlayer != null ? blackPlayer.getDisplayName() : "Unknown");
        response.put("pgn", match.getPgn());
        response.put("status", match.getStatus());

        response.put("timeControl", match.getTimeControl());

        return ResponseEntity.ok(response);
    }

    @Autowired
    private com.ptit.chess.service.MatchService matchService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @PostMapping("/room/{roomId}/rematch/{oldMatchId}")
    public ResponseEntity<?> requestRematch(@PathVariable Long roomId, @PathVariable Long oldMatchId) {
        try {
            Match match = matchService.startMatch(roomId);
            messagingTemplate.convertAndSend("/topic/match/" + oldMatchId, "MATCH_STARTED");
            return ResponseEntity.ok("Rematch started");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
