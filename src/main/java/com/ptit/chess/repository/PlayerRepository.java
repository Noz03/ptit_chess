package com.ptit.chess.repository;

import com.ptit.chess.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByAccountId(Long accountId);
    boolean existsByDisplayName(String displayName);
    List<Player> findTop50ByOrderByEloDesc();
    long countByActivityStatusIn(List<com.ptit.chess.entity.ActivityStatus> statuses);
}
