package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.entity.Room;

import java.util.List;

public interface RoomService {
    List<Room> getAllRooms();
    Room getRoomById(Long id);
    Room createRoom(Room room);
    Room updateRoom(Long id, Room roomDetails);
    void deleteRoomById(Long id);
}
