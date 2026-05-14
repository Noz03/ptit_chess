package com.ptit.chess.repository;

import com.ptit.chess.entity.Match;
import com.ptit.chess.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByRoomIdAndStatus(Long roomId, MatchStatus status);
    Optional<Match> findFirstByRoomIdOrderByStartTimeDesc(Long roomId);
    List<Match> findByWhitePlayerIdOrBlackPlayerId(Long whitePlayerId, Long blackPlayerId);
}
