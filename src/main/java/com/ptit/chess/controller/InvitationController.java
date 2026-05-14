package com.ptit.chess.controller;

import com.ptit.chess.dto.SendInvitationRequest;
import com.ptit.chess.entity.Invitation;
import com.ptit.chess.entity.InvitationStatus;
import com.ptit.chess.entity.Player;
import com.ptit.chess.entity.Room;
import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.repository.InvitationRepository;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.repository.RoomRepository;
import com.ptit.chess.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<?> sendInvitation(@RequestBody SendInvitationRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player sender = userDetails.getAccount().getPlayer();

        if (sender.getId().equals(request.getReceiverId())) {
            return ResponseEntity.badRequest().body("Cannot invite yourself.");
        }

        Room room = roomRepository.findById(request.getRoomId()).orElse(null);
        if (room == null || !room.getHostPlayerId().equals(sender.getId()) || room.getStatus() != RoomStatus.WAITING) {
            return ResponseEntity.badRequest().body("Invalid room.");
        }

        Player receiver = playerRepository.findById(request.getReceiverId()).orElse(null);
        if (receiver == null) {
            return ResponseEntity.badRequest().body("Receiver not found.");
        }

        Invitation invitation = Invitation.builder()
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .roomId(room.getId())
                .status(InvitationStatus.PENDING)
                .build();

        invitation = invitationRepository.save(invitation);

        // Notify receiver via WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", invitation.getId());
        payload.put("senderName", sender.getDisplayName());
        payload.put("roomId", room.getId());
        payload.put("roomCode", room.getRoomCode());
        
        messagingTemplate.convertAndSend("/topic/player/" + receiver.getId(), (Object) payload);

        return ResponseEntity.ok("Invitation sent successfully.");
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player receiver = userDetails.getAccount().getPlayer();

        Optional<Invitation> optInv = invitationRepository.findById(id);
        if (optInv.isEmpty() || !optInv.get().getReceiverId().equals(receiver.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid invitation.");
        }

        Invitation invitation = optInv.get();
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body("Invitation is no longer pending.");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // Room joining logic is already handled by RoomController join endpoint,
        // so the frontend should just call joinRoom with the roomCode after accepting.
        // We will return the roomCode to the client.
        Room room = roomRepository.findById(invitation.getRoomId()).orElse(null);
        if (room == null) return ResponseEntity.badRequest().body("Room no longer exists.");

        Map<String, String> response = new HashMap<>();
        response.put("roomCode", room.getRoomCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player receiver = userDetails.getAccount().getPlayer();

        Optional<Invitation> optInv = invitationRepository.findById(id);
        if (optInv.isEmpty() || !optInv.get().getReceiverId().equals(receiver.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid invitation.");
        }

        Invitation invitation = optInv.get();
        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        return ResponseEntity.ok("Invitation rejected.");
    }
}
