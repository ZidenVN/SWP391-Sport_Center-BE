package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.RoomRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.RoomResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.RoomRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import com.fptu.swp391.sportscentermanager.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final SportClassRepository sportClassRepository;
    private final ImageStorageService imageStorageService;

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findByStatusNot("INACTIVE");
    }

    @Override
    public Room getRoomById(Long id) {
       return roomRepository.findByRoomIdAndStatusNot(id, "INACTIVE").orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Override
    public RoomResponseDTO createRoom(RoomRequestDTO requestDTO) {
        String status = (requestDTO.getStatus() != null && !requestDTO.getStatus().isBlank())
            ? requestDTO.getStatus().trim()
            : "AVAILABLE";

        Room newRoom = Room.builder()
            .roomName(requestDTO.getRoomName())
            .capacity(requestDTO.getCapacity())
            .status(status)
            .build();

        Room savedRoom = roomRepository.save(newRoom);
        return toDTO(savedRoom);
    }

    @Override
    public RoomResponseDTO updateRoom(Long id, RoomRequestDTO requestDTO) {
        Room existingRoom = getRoomById(id);
        if (sportClassRepository.existsByRoom_RoomIdAndStatusAndMaxCapacityGreaterThan(id, "OPENING", requestDTO.getCapacity())) {
            throw new AppException(ErrorCode.ROOM_CAPACITY_BELOW_CLASS);
        }

        existingRoom.setRoomName(requestDTO.getRoomName());
        existingRoom.setCapacity(requestDTO.getCapacity());
        if (requestDTO.getStatus() != null && !requestDTO.getStatus().isBlank()) {
            String newStatus = requestDTO.getStatus().trim();
            // Không cho chuyển sang bảo trì khi phòng còn lớp sắp diễn ra
            if ("MAINTENANCE".equals(newStatus)
                && !"MAINTENANCE".equals(existingRoom.getStatus())
                && sportClassRepository.existsByRoom_RoomIdAndStatusAndStartTimeAfter(id, "OPENING", LocalDateTime.now())) {
                throw new AppException(ErrorCode.ROOM_IN_USE);
            }
            existingRoom.setStatus(newStatus);
        }

        Room updatedRoom = roomRepository.save(existingRoom);
        return toDTO(updatedRoom);
    }

    @Override
    public void deleteRoomById(Long id) {
        Room room = getRoomById(id);
        if (sportClassRepository.existsByRoom_RoomIdAndStatusAndStartTimeAfter(id, "OPENING", LocalDateTime.now())) {
            throw new AppException(ErrorCode.ROOM_IN_USE);
        }
        roomRepository.delete(room);
    }

    @Override
    public RoomResponseDTO updateRoomImage(Long id, MultipartFile image) {
        // tìm phòng trước khi upload: id sai thì không đẩy ảnh rác lên Cloudinary
        Room room = getRoomById(id);
        // ảnh cũ giữ nguyên trên Cloudinary (giống luật ảnh lớp/môn), chỉ thay URL
        room.setImageUrl(imageStorageService.uploadRoomImage(image));
        return toDTO(roomRepository.save(room));
    }

    private RoomResponseDTO toDTO(Room room) {
        return RoomResponseDTO.builder()
            .roomId(room.getRoomId())
            .roomName(room.getRoomName())
            .capacity(room.getCapacity())
            .status(room.getStatus())
            .imageUrl(room.getImageUrl())
            .build();
    }
}
