package com.ptit.chess.controller;

import com.ptit.chess.dto.AccountDto;
import com.ptit.chess.dto.AdminStatsDto;
import com.ptit.chess.entity.Account;
import com.ptit.chess.entity.AccountStatus;
import com.ptit.chess.entity.ActivityStatus;
import com.ptit.chess.entity.Match;
import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.repository.AccountRepository;
import com.ptit.chess.repository.MatchRepository;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MatchRepository matchRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalAccounts = accountRepository.count();
        long lockedAccounts = accountRepository.countByStatus(AccountStatus.LOCKED);
        long onlinePlayers = playerRepository.countByActivityStatusIn(List.of(ActivityStatus.ONLINE, ActivityStatus.PLAYING));
        long openRooms = roomRepository.countByStatus(RoomStatus.WAITING);
        long totalMatches = matchRepository.count();

        AdminStatsDto stats = AdminStatsDto.builder()
                .totalAccounts(totalAccounts)
                .lockedAccounts(lockedAccounts)
                .onlinePlayers(onlinePlayers)
                .openRooms(openRooms)
                .totalMatches(totalMatches)
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        List<AccountDto> accountDtos = accounts.stream().map(acc -> {
            Integer elo = 0, win = 0, draw = 0, loss = 0;
            String displayName = "";
            if (acc.getPlayer() != null) {
                elo = acc.getPlayer().getElo();
                win = acc.getPlayer().getWinCount();
                draw = acc.getPlayer().getDrawCount();
                loss = acc.getPlayer().getLossCount();
                displayName = acc.getPlayer().getDisplayName();
            }
            return AccountDto.builder()
                    .id(acc.getId())
                    .username(acc.getUsername())
                    .role(acc.getRole())
                    .status(acc.getStatus())
                    .displayName(displayName)
                    .elo(elo)
                    .winCount(win)
                    .drawCount(draw)
                    .lossCount(loss)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(accountDtos);
    }

    @PutMapping("/accounts/{id}/status")
    public ResponseEntity<?> changeAccountStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        String newStatus = body.get("status");
        if ("LOCKED".equals(newStatus)) {
            account.setStatus(AccountStatus.LOCKED);
        } else if ("ACTIVE".equals(newStatus)) {
            account.setStatus(AccountStatus.ACTIVE);
        }

        accountRepository.save(account);
        return ResponseEntity.ok("Status updated successfully");
    }

    @GetMapping("/matches")
    public ResponseEntity<?> getAllMatches() {
        List<Match> matches = matchRepository.findAll();
        matches.sort((a, b) -> b.getId().compareTo(a.getId()));
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/matches/search")
    public ResponseEntity<?> searchMatches(
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String reason) {
        List<Match> matches = matchRepository.findAll();
        matches.sort((a, b) -> b.getId().compareTo(a.getId()));
        return ResponseEntity.ok(matches.stream().filter(m -> {
            boolean ok = true;
            if (result != null && !result.isEmpty()) {
                ok = ok && m.getResult() != null && m.getResult().name().equalsIgnoreCase(result);
            }
            if (reason != null && !reason.isEmpty()) {
                ok = ok && m.getEndReason() != null && m.getEndReason().name().equalsIgnoreCase(reason);
            }
            return ok;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/accounts/{id}/role")
    public ResponseEntity<?> changeAccountRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        com.ptit.chess.entity.Account account = accountRepository.findById(id).orElse(null);
        if (account == null) return ResponseEntity.notFound().build();
        String newRole = body.get("role");
        try {
            account.setRole(com.ptit.chess.entity.Role.valueOf(newRole));
            accountRepository.save(account);
            return ResponseEntity.ok("Role updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role: " + newRole);
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getActiveRooms() {
        return ResponseEntity.ok(roomRepository.findAll().stream()
            .filter(r -> r.getStatus() != RoomStatus.CLOSED)
            .map(r -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("name", r.getRoomCode());
                map.put("status", r.getStatus());
                map.put("roomType", r.getRoomType());
                map.put("timeControl", r.getTimeControl());
                map.put("hostPlayerId", r.getHostPlayerId());
                map.put("guestPlayerId", r.getGuestPlayerId());
                return map;
            }).collect(Collectors.toList()));
    }

    @GetMapping("/players/online")
    public ResponseEntity<?> getOnlinePlayers() {
        return ResponseEntity.ok(playerRepository.findAll().stream()
            .filter(p -> p.getActivityStatus() == ActivityStatus.ONLINE || p.getActivityStatus() == ActivityStatus.PLAYING)
            .map(p -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", p.getId());
                map.put("displayName", p.getDisplayName());
                map.put("avatarUrl", p.getAvatarUrl());
                map.put("elo", p.getElo());
                map.put("activityStatus", p.getActivityStatus());
                return map;
            }).collect(Collectors.toList()));
    }
}
