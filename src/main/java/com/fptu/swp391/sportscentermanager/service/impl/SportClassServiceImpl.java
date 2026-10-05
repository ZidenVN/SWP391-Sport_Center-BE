package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.CoachRepository;
import com.fptu.swp391.sportscentermanager.repository.RoomRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.service.RoomService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportClassServiceImpl implements SportClassService {
    private final SportClassRepository sportClassRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final CoachRepository coachRepository;

    @Override
    public SportClassResponseDTO createClass(SportClassRequestDTO requestDTO) {
       Room room = roomRepository.findById(requestDTO.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

       Subject subject = subjectRepository.findById(requestDTO.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

       Coach coach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        SportClass newClass = SportClass.builder().maxCapacity(requestDTO.getMaxCapacity()).scheduleTime(requestDTO.getScheduleTime()).status("OPENING").room(room).subject(subject).coach(coach).build();

        SportClass savedClass = sportClassRepository.save(newClass);

        return SportClassResponseDTO.builder().classId(savedClass.getClassId()).maxCapacity(savedClass.getMaxCapacity()).scheduleTime(savedClass.getScheduleTime()).status(savedClass.getStatus()).roomName(savedClass.getRoom().getRoomName()).subjectName(savedClass.getSubject().getSubjectName()).coachName(savedClass.getCoach().getFirstName() + " " + savedClass.getCoach().getLastName()).build();
    }

    @Override
    public SportClass getClassById(Long id) {
        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    @Override
    public List<SportClassResponseDTO> getAllClasses() {
        List<SportClass> classList = sportClassRepository.findAll();
        List<SportClassResponseDTO> result = new ArrayList<>();

        for(SportClass c : classList){
            SportClassResponseDTO dto = SportClassResponseDTO.builder()
                .classId(c.getClassId())
                .maxCapacity(c.getMaxCapacity())
                .scheduleTime(c.getScheduleTime())
                .status(c.getStatus())
                .roomName(c.getRoom().getRoomName())
                .subjectName(c.getSubject().getSubjectName())
                .coachName(c.getCoach().getFirstName() + " " + c.getCoach().getLastName()).build();
            result.add(dto);
        }
        return result;
    }

    @Override
    public SportClassResponseDTO assignCoachToClass(Long classId, Long coachId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        sportClass.setCoach(coach);
        SportClass savedClass = sportClassRepository.save(sportClass);

        return SportClassResponseDTO.builder()
            .classId(savedClass.getClassId())
            .maxCapacity(savedClass.getMaxCapacity())
            .scheduleTime(savedClass.getScheduleTime())
            .status(savedClass.getStatus())
            .roomName(savedClass.getRoom().getRoomName())
            .subjectName(savedClass.getSubject().getSubjectName())
            .coachName(savedClass.getCoach().getFirstName() + " " + savedClass.getCoach().getLastName())
            .build();
    }

    @Override
    public void deleteClassById(Long id) {
        SportClass sportClass = getClassById(id);
        sportClass.setStatus("INACTIVE");
        sportClassRepository.delete(sportClass);
    }
}
