package com.ptit.chess.controller;

import com.ptit.chess.dto.PlayerProfileDto;
import com.ptit.chess.dto.PlayerUpdateRequest;
import com.ptit.chess.entity.Player;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();
        
        PlayerProfileDto profile = PlayerProfileDto.builder()
                .id(player.getId())
                .username(userDetails.getUsername())
                .displayName(player.getDisplayName())
                .avatarUrl(player.getAvatarUrl())
                .elo(player.getElo())
                .winCount(player.getWinCount())
                .drawCount(player.getDrawCount())
                .lossCount(player.getLossCount())
                .activityStatus(player.getActivityStatus())
                .build();
                
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody PlayerUpdateRequest updateRequest, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        if (updateRequest.getDisplayName() != null && !updateRequest.getDisplayName().isEmpty() 
            && !updateRequest.getDisplayName().equals(player.getDisplayName())) {
            if (playerRepository.existsByDisplayName(updateRequest.getDisplayName())) {
                return ResponseEntity.badRequest().body("Display name is already taken!");
            }
            player.setDisplayName(updateRequest.getDisplayName());
        }

        if (updateRequest.getAvatarUrl() != null) {
            player.setAvatarUrl(updateRequest.getAvatarUrl());
        }

        playerRepository.save(player);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        List<Player> topPlayers = playerRepository.findTop50ByOrderByEloDesc();
        List<PlayerProfileDto> leaderboard = topPlayers.stream().map(player -> PlayerProfileDto.builder()
                .id(player.getId())
                .username(player.getAccount().getUsername())
                .displayName(player.getDisplayName())
                .avatarUrl(player.getAvatarUrl())
                .elo(player.getElo())
                .winCount(player.getWinCount())
                .drawCount(player.getDrawCount())
                .lossCount(player.getLossCount())
                .activityStatus(player.getActivityStatus())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getMatchHistory(Authentication authentication) {
        // Stub for now. Assuming we have a Match entity and repository.
        // For now, return an empty list.
        return ResponseEntity.ok(new ArrayList<>());
    }
}
