package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.RoomRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.RoomResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Room;

import java.util.List;

public interface RoomService {
    List<Room> getAllRooms();
    Room getRoomById(Long id);
    RoomResponseDTO createRoom(RoomRequestDTO requestDTO);
    RoomResponseDTO updateRoom(Long id, RoomRequestDTO RequestDTO);
    void deleteRoomById(Long id);
}
