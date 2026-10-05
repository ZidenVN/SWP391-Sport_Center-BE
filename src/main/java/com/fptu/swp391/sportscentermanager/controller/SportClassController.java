package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class SportClassController {
    private final SportClassService sportClassService;

    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PostMapping
    public ResponseEntity<SportClassResponseDTO> createClass(@RequestBody SportClassRequestDTO requestDTO){
        return   ResponseEntity.ok(sportClassService.createClass(requestDTO));
    }

    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping
    public ResponseEntity<List<SportClassResponseDTO>> getAllClasses(){
        return ResponseEntity.ok(sportClassService.getAllClasses());
    }

    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping("/{id}")
    public ResponseEntity<SportClassResponseDTO> getClassById(@PathVariable Long id){
        return ResponseEntity.ok(sportClassService.getSportClassDTOById(id));
    }


    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @PutMapping("/{classId}/coach/{coachId}")
    public ResponseEntity<SportClassResponseDTO> assignCoachToClass(@PathVariable Long classId, @PathVariable Long coachId){
        return ResponseEntity.ok(sportClassService.assignCoachToClass(classId, coachId));
    }

    @PreAuthorize("hasAuthority('MANAGE_CLASS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClass(@PathVariable Long id){
        sportClassService.deleteClassById(id);
        return ResponseEntity.ok("Đã xóa lớp học thành công!");
    }


}
