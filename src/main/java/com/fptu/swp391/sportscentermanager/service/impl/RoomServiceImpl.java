package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.RoomRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.RoomResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.RoomRepository;
import com.fptu.swp391.sportscentermanager.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Room getRoomById(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Override
    public RoomResponseDTO createRoom(RoomRequestDTO requestDTO) {
        Room newRoom = Room.builder()
            .roomName(requestDTO.getRoomName())
            .capacity(requestDTO.getCapacity())
            .status("AVAILABLE")
            .build();

        Room savedRoom = roomRepository.save(newRoom);
        return RoomResponseDTO.builder()
            .roomId(savedRoom.getRoomId())
            .roomName(savedRoom.getRoomName())
            .capacity(savedRoom.getCapacity())
            .status(savedRoom.getStatus())
            .build();
    }

    @Override
    public RoomResponseDTO updateRoom(Long id, RoomRequestDTO requestDTO) {
        Room existingRoom = getRoomById(id);
        existingRoom.setRoomName(requestDTO.getRoomName());
        existingRoom.setCapacity(requestDTO.getCapacity());

        Room updatedRoom = roomRepository.save(existingRoom);
        return RoomResponseDTO.builder().
            roomName(updatedRoom.getRoomName())
            .capacity(updatedRoom.getCapacity())
            .status(updatedRoom.getStatus())
            .build();
    }

    @Override
    public void deleteRoomById(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }
}
