package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.RoomRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.RoomResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.repository.RoomRepository;
import com.fptu.swp391.sportscentermanager.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROOM')")
    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@RequestBody RoomRequestDTO requestDTO) {
        return ResponseEntity.ok(roomService.createRoom(requestDTO));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROOM')")
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> updateRoom(@PathVariable Long id, @RequestBody RoomRequestDTO roomRequestDTO) throws  Exception{
        return ResponseEntity.ok(roomService.updateRoom(id, roomRequestDTO));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROOM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoomById(@PathVariable Long id) {
        roomService.deleteRoomById(id);
        return ResponseEntity.ok("Đã xóa phòng tập thành công!");
    }
}
