package com.ptit.chess.listener;

import com.ptit.chess.entity.ActivityStatus;
import com.ptit.chess.entity.Player;
import com.ptit.chess.entity.Room;
import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.repository.RoomRepository;
import com.ptit.chess.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Optional;

@Component
public class WebSocketEventListener {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        if (headerAccessor.getUser() != null) {
            Authentication auth = (Authentication) headerAccessor.getUser();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long playerId = userDetails.getAccount().getPlayer().getId();

            // Set player offline
            Optional<Player> optPlayer = playerRepository.findById(playerId);
            if (optPlayer.isPresent()) {
                Player player = optPlayer.get();
                player.setActivityStatus(ActivityStatus.OFFLINE);
                playerRepository.save(player);
            }

            // Clean up rooms where the player is HOST and the room is WAITING
            List<Room> activeRooms = roomRepository.findAll();
            for (Room room : activeRooms) {
                if (room.getStatus() == RoomStatus.WAITING && room.getHostPlayerId().equals(playerId)) {
                    room.setStatus(RoomStatus.CLOSED);
                    roomRepository.save(room);
                    // Broadcast update so lobby can remove it
                    messagingTemplate.convertAndSend("/topic/lobby", "ROOM_UPDATED");
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(org.springframework.web.socket.messaging.SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        if (headerAccessor.getUser() != null) {
            Authentication auth = (Authentication) headerAccessor.getUser();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long playerId = userDetails.getAccount().getPlayer().getId();

            Optional<Player> optPlayer = playerRepository.findById(playerId);
            if (optPlayer.isPresent()) {
                Player player = optPlayer.get();
                player.setActivityStatus(ActivityStatus.ONLINE);
                playerRepository.save(player);
            }
        }
    }
}
