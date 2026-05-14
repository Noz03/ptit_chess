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

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            // Ensure uploads directory exists
            String uploadDir = "src/main/resources/static/uploads/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save file
            String filename = player.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir + filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Update player
            String avatarUrl = "/uploads/" + filename;
            player.setAvatarUrl(avatarUrl);
            playerRepository.save(player);

            return ResponseEntity.ok(avatarUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload avatar");
        }
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlinePlayers(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getAccount().getPlayer().getId();

        List<Player> onlinePlayers = playerRepository.findByActivityStatus(com.ptit.chess.entity.ActivityStatus.ONLINE);
        
        List<PlayerProfileDto> dtos = onlinePlayers.stream()
                .filter(p -> !p.getId().equals(currentUserId))
                .map(p -> PlayerProfileDto.builder()
                        .id(p.getId())
                        .displayName(p.getDisplayName())
                        .avatarUrl(p.getAvatarUrl())
                        .elo(p.getElo())
                        .winCount(p.getWinCount())
                        .drawCount(p.getDrawCount())
                        .lossCount(p.getLossCount())
                        .activityStatus(p.getActivityStatus())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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
