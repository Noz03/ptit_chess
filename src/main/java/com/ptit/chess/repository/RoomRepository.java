package com.ptit.chess.repository;

import com.ptit.chess.entity.Room;
import com.ptit.chess.entity.RoomStatus;
import com.ptit.chess.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);
    List<Room> findByRoomTypeAndStatus(RoomType roomType, RoomStatus status);
    
    @Query("SELECT r FROM Room r WHERE (r.hostPlayerId = :playerId OR r.guestPlayerId = :playerId) AND r.status IN ('WAITING', 'FULL', 'PLAYING')")
    List<Room> findActiveRoomsByPlayerId(Long playerId);
    
    long countByStatus(RoomStatus status);
}
