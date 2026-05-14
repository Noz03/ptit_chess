package com.ptit.chess.repository;

import com.ptit.chess.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByStatus(com.ptit.chess.entity.AccountStatus status);
}
