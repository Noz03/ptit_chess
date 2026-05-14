package com.ptit.chess.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.ptit.chess.entity.*;
import com.ptit.chess.repository.MatchRepository;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EloService eloService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Random random = new Random();

    @Transactional
    public Match startMatch(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.getStatus() != RoomStatus.FULL) {
            throw new IllegalStateException("Room is not full");
        }

        // Check if there's already an active match
        Optional<Match> activeMatch = matchRepository.findByRoomIdAndStatus(roomId, MatchStatus.IN_PROGRESS);
        if (activeMatch.isPresent()) {
            return activeMatch.get();
        }

        boolean hostIsWhite = random.nextBoolean();
        Long whitePlayerId = hostIsWhite ? room.getHostPlayerId() : room.getGuestPlayerId();
        Long blackPlayerId = hostIsWhite ? room.getGuestPlayerId() : room.getHostPlayerId();

        Match match = Match.builder()
                .roomId(roomId)
                .whitePlayerId(whitePlayerId)
                .blackPlayerId(blackPlayerId)
                .status(MatchStatus.IN_PROGRESS)
                .timeControl(room.getTimeControl())
                .pgn("") // Initial empty PGN
                .build();

        match = matchRepository.save(match);
        
        room.setStatus(RoomStatus.PLAYING);
        roomRepository.save(room);

        // Notify room members
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "MATCH_STARTED");
        messagingTemplate.convertAndSend("/topic/lobby", "ROOM_UPDATED");

        return match;
    }

    @Transactional
    public void handleMove(Long matchId, Long playerId, String moveSan, String currentPgn) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Match is not in progress");
        }

        // In a strictly secure backend, we would validate the move using chesslib.
        // Board board = new Board();
        // Game game = Game.loadPgn(match.getPgn());
        // board.doMove(moveSan);
        
        // As agreed, we will trust the frontend's PGN tracking for simplicity but maintain the Match state.
        match.setPgn(currentPgn);
        matchRepository.save(match);

        // Broadcast move
        messagingTemplate.convertAndSend("/topic/match/" + matchId, "MOVE:" + currentPgn);
    }

    @Transactional
    public void endGame(Long matchId, MatchResult result, MatchEndReason reason) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            return;
        }

        match.setStatus(MatchStatus.FINISHED);
        match.setResult(result);
        match.setEndReason(reason);
        match.setEndTime(LocalDateTime.now());
        matchRepository.save(match);

        Player whitePlayer = playerRepository.findById(match.getWhitePlayerId()).orElseThrow();
        Player blackPlayer = playerRepository.findById(match.getBlackPlayerId()).orElseThrow();

        double whiteScore = 0.5;
        if (result == MatchResult.WHITE_WIN) whiteScore = 1.0;
        else if (result == MatchResult.BLACK_WIN) whiteScore = 0.0;

        eloService.updateRatings(whitePlayer, blackPlayer, whiteScore);

        playerRepository.save(whitePlayer);
        playerRepository.save(blackPlayer);

        Room room = roomRepository.findById(match.getRoomId()).orElse(null);
        if (room != null) {
            room.setStatus(RoomStatus.FULL); // Keep guest in room for potential rematch
            roomRepository.save(room);
        }

        // Notify clients
        messagingTemplate.convertAndSend("/topic/match/" + matchId, "GAME_OVER:" + result.name() + ":" + reason.name());
        if (room != null) {
            messagingTemplate.convertAndSend("/topic/room/" + room.getId(), "MATCH_ENDED");
            messagingTemplate.convertAndSend("/topic/lobby", "ROOM_UPDATED");
        }
    }
    @Transactional
    public void handleResign(Long matchId, Long playerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        if (match.getStatus() != MatchStatus.IN_PROGRESS) return;

        MatchResult result = match.getWhitePlayerId().equals(playerId) ? MatchResult.BLACK_WIN : MatchResult.WHITE_WIN;
        endGame(matchId, result, MatchEndReason.RESIGN);
    }
}
