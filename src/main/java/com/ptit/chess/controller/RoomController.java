package com.ptit.chess.controller;

import com.ptit.chess.dto.CreateRoomRequest;
import com.ptit.chess.dto.RoomDto;
import com.ptit.chess.entity.Player;
import com.ptit.chess.entity.Room;
import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.entity.RoomType;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.repository.RoomRepository;
import com.ptit.chess.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.ptit.chess.service.MatchService matchService;

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player host = userDetails.getAccount().getPlayer();

        // Block if player is already in an active room
        List<Room> activeRooms = roomRepository.findActiveRoomsByPlayerId(host.getId());
        if (!activeRooms.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("You are already in an active room.");
        }

        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Room room = Room.builder()
                .roomCode(roomCode)
                .hostPlayerId(host.getId())
                .roomType(request.getRoomType() != null ? request.getRoomType() : RoomType.OPEN)
                .timeControl(request.getTimeControl() != null ? request.getTimeControl() : 600)
                .status(RoomStatus.WAITING)
                .build();

        room = roomRepository.save(room);

        RoomDto roomDto = toRoomDto(room);
        messagingTemplate.convertAndSend("/topic/lobby", "ROOM_CREATED"); // Broadcast to lobby if needed

        return ResponseEntity.ok(roomDto);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRoom(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        List<Room> activeRooms = roomRepository.findActiveRoomsByPlayerId(player.getId());
        if (!activeRooms.isEmpty()) {
            return ResponseEntity.ok(toRoomDto(activeRooms.get(0)));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lobby")
    public ResponseEntity<?> getLobbyRooms() {
        List<Room> openRooms = roomRepository.findByRoomTypeAndStatus(RoomType.OPEN, RoomStatus.WAITING);
        List<RoomDto> roomDtos = openRooms.stream()
                .map(this::toRoomDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }

    @PostMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomCode, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player guest = userDetails.getAccount().getPlayer();

        // Block if player is already in an active room
        List<Room> activeRooms = roomRepository.findActiveRoomsByPlayerId(guest.getId());
        if (!activeRooms.isEmpty()) {
            // But if the player is already in THIS room, allow them to re-enter
            boolean alreadyInThisRoom = activeRooms.stream().anyMatch(r -> r.getRoomCode().equals(roomCode));
            if (!alreadyInThisRoom) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("You are already in another active room.");
            }
        }

        Optional<Room> optionalRoom = roomRepository.findByRoomCode(roomCode);
        if (optionalRoom.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }

        Room room = optionalRoom.get();

        if (room.getHostPlayerId().equals(guest.getId())) {
            // Host is re-joining
            return ResponseEntity.ok(toRoomDto(room));
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            if (guest.getId().equals(room.getGuestPlayerId())) {
                // Guest is re-joining
                return ResponseEntity.ok(toRoomDto(room));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Room is not waiting for players.");
        }

        room.setGuestPlayerId(guest.getId());
        room.setStatus(RoomStatus.FULL);
        roomRepository.save(room);

        RoomDto roomDto = toRoomDto(room);
        
        // Notify the room via WebSocket
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), roomDto);
        // Notify lobby
        messagingTemplate.convertAndSend("/topic/lobby", "ROOM_UPDATED");

        // Automatically start match
        matchService.startMatch(room.getId());

        return ResponseEntity.ok(roomDto);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Player player = userDetails.getAccount().getPlayer();

        Optional<Room> optionalRoom = roomRepository.findById(roomId);
        if (optionalRoom.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }

        Room room = optionalRoom.get();

        if (room.getHostPlayerId().equals(player.getId())) {
            room.setStatus(RoomStatus.CLOSED);
        } else if (player.getId().equals(room.getGuestPlayerId())) {
            room.setGuestPlayerId(null);
            room.setStatus(RoomStatus.WAITING);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not in this room.");
        }

        roomRepository.save(room);
        RoomDto roomDto = toRoomDto(room);

        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), roomDto);
        messagingTemplate.convertAndSend("/topic/lobby", "ROOM_UPDATED");

        return ResponseEntity.ok("Left room successfully");
    }

    private RoomDto toRoomDto(Room room) {
        String hostName = "Unknown";
        if (room.getHostPlayerId() != null) {
            hostName = playerRepository.findById(room.getHostPlayerId()).map(Player::getDisplayName).orElse("Unknown");
        }
        
        String guestName = null;
        if (room.getGuestPlayerId() != null) {
            guestName = playerRepository.findById(room.getGuestPlayerId()).map(Player::getDisplayName).orElse("Unknown");
        }

        return RoomDto.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .hostPlayerId(room.getHostPlayerId())
                .hostDisplayName(hostName)
                .guestPlayerId(room.getGuestPlayerId())
                .guestDisplayName(guestName)
                .roomType(room.getRoomType())
                .status(room.getStatus())
                .timeControl(room.getTimeControl())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
