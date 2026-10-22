package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportClassServiceImpl implements SportClassService {
    private final SportClassRepository sportClassRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final CoachRepository coachRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter SCHEDULE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public SportClassResponseDTO createClass(SportClassRequestDTO requestDTO) {
        Room room = roomRepository.findById(requestDTO.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        Subject subject = subjectRepository.findById(requestDTO.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // TODO: Xử lý logic gán Coach sau khi hoàn thiện module Coach
        Coach coach = null;
        if (requestDTO.getCoachId() != null) {
            coach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        }

        validateClassTime(requestDTO.getStartTime(), requestDTO.getEndTime());

        validateRoomForClass(room, requestDTO.getMaxCapacity());

        checkScheduleConflict(requestDTO.getRoomId(), requestDTO.getCoachId(), requestDTO.getStartTime(), requestDTO.getEndTime(), 0L);

        SportClass newClass = SportClass.builder().maxCapacity(requestDTO.getMaxCapacity()).startTime(requestDTO.getStartTime()).endTime(requestDTO.getEndTime()).status("OPENING").room(room).subject(subject).coach(coach).build();

        SportClass savedClass = sportClassRepository.save(newClass);

        return toResponseDTO(savedClass);
    }

    @Override
    public SportClass getClassById(Long id) {
        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    @Override
    public SportClassResponseDTO getSportClassById(Long id) {
        SportClass c = getClassById(id);
       return toResponseDTO(getClassById(id));
    }


    @Override
    public List<SportClassResponseDTO> getAllClasses() {
        return sportClassRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    public SportClassResponseDTO assignCoachToClass(Long classId, Long coachId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (coach.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }

        if (sportClassRepository.existsCoachConflict(coachId, sportClass.getStartTime(), sportClass.getEndTime(), classId)) {
            throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
        }

        sportClass.setCoach(coach);
        SportClass savedClass = sportClassRepository.save(sportClass);

        return toResponseDTO(savedClass);
    }

    @Override
    @Transactional
    public SportClassResponseDTO updateClass(Long id, SportClassRequestDTO requestDTO) {
        SportClass sportClass = sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        LocalDateTime oldStart = sportClass.getStartTime();
        LocalDateTime oldEnd = sportClass.getEndTime();
        Long oldRoomId = sportClass.getRoom().getRoomId();

        validateClassTime(requestDTO.getStartTime(), requestDTO.getEndTime());

        checkScheduleConflict(requestDTO.getRoomId(), requestDTO.getCoachId(), requestDTO.getStartTime(), requestDTO.getEndTime(), id);

        sportClass.setMaxCapacity(requestDTO.getMaxCapacity());
        sportClass.setStartTime(requestDTO.getStartTime());
        sportClass.setEndTime(requestDTO.getEndTime());

        Room room = sportClass.getRoom().getRoomId().equals(requestDTO.getRoomId()) ? sportClass.getRoom() : roomRepository.findById(requestDTO.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        validateRoomForClass(room, requestDTO.getMaxCapacity());

        long registered = classRegistrationRepository.countBySportClass_ClassIdAndStatus(id, "REGISTERED");
        if (requestDTO.getMaxCapacity() < registered) {
            throw new AppException(ErrorCode.CAPACITY_BELOW_REGISTERED);
        }

        sportClass.setRoom(room);

        if (!sportClass.getSubject().getSubjectId().equals(requestDTO.getSubjectId())) {
            Subject newSubject = subjectRepository.findById(requestDTO.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
            sportClass.setSubject(newSubject);
        }

        if (requestDTO.getCoachId() != null) {
            Coach newCoach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
            sportClass.setCoach(newCoach);
        } else {
            sportClass.setCoach(null);
        }

        SportClass savedClass = sportClassRepository.save(sportClass);

        boolean timeChanged = !oldStart.equals(savedClass.getStartTime()) || !oldEnd.equals(savedClass.getEndTime());
        boolean roomChanged = !oldRoomId.equals(savedClass.getRoom().getRoomId());
        if (timeChanged || roomChanged) {
            notificationService.sendSystemNotification(
                registeredMembers(id),
                "Lớp " + savedClass.getSubject().getSubjectName() + " thay đổi lịch",
                "Lớp " + savedClass.getSubject().getSubjectName() + " bạn đã đăng ký được chuyển sang "
                    + savedClass.getStartTime().format(SCHEDULE_FORMAT) + " - " + savedClass.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + " tại " + savedClass.getRoom().getRoomName()
                    + " (lịch cũ: " + oldStart.format(SCHEDULE_FORMAT) + ").");
        }

        return getSportClassById(savedClass.getClassId());
    }

    // Xóa mềm (SportClass đã có @SQLDelete/@SQLRestriction): hủy các đăng ký còn hiệu lực, báo cho học viên,
    // rồi chuyển lớp sang INACTIVE. Dùng save thay vì delete vì các đăng ký vừa nạp vẫn tham chiếu tới lớp
    // trong cùng session, delete sẽ làm Hibernate báo TransientPropertyValueException.
    @Override
    @Transactional
    public void deleteClass(Long id) {
        SportClass sportClass = getClassById(id);
        List<ClassRegistration> activeRegistrations = classRegistrationRepository.findActiveByClassId(id);
        LocalDateTime now = LocalDateTime.now();
        activeRegistrations.forEach(r -> {
            r.setStatus("CANCELLED");
            r.setCancelledAt(now);
        });
        classRegistrationRepository.saveAll(activeRegistrations);

        notificationService.sendSystemNotification(
            activeRegistrations.stream().map(ClassRegistration::getMember).toList(),
            "Lớp " + sportClass.getSubject().getSubjectName() + " đã bị hủy",
            "Lớp " + sportClass.getSubject().getSubjectName() + " lúc " + sportClass.getStartTime().format(SCHEDULE_FORMAT)
                + " đã bị hủy, đăng ký của bạn được hủy tự động. Vui lòng chọn lớp khác.");

        sportClass.setStatus("INACTIVE");
        sportClassRepository.save(sportClass);
    }

    private List<Member> registeredMembers(Long classId) {
        return classRegistrationRepository.findActiveByClassId(classId).stream()
            .map(ClassRegistration::getMember)
            .toList();
    }

    private void validateClassTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new AppException(ErrorCode.INVALID_CLASS_TIME);
        }
    }

    private void checkScheduleConflict(Long roomId, Long coachId, LocalDateTime startTime, LocalDateTime endTime, Long excludeClassId) {
        if (sportClassRepository.existsRoomConflict(roomId, startTime, endTime, excludeClassId)) {
            throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
        }
        if (coachId != null && sportClassRepository.existsCoachConflict(coachId, startTime, endTime, excludeClassId)) {
            throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
        }
    }

    private void validateRoomForClass(Room room, Integer maxCapacity) {
        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }

        if (maxCapacity > room.getCapacity()) {
            throw new AppException(ErrorCode.CLASS_CAPACITY_EXCEEDS_ROOM);
        }
    }

    private SportClassResponseDTO toResponseDTO(SportClass c) {
        long enrolled = classRegistrationRepository.countBySportClass_ClassIdAndStatus(c.getClassId(), "REGISTERED");
        return SportClassResponseDTO.builder().classId(c.getClassId()).maxCapacity(c.getMaxCapacity())
            .currentEnrolled(enrolled)
            .availableSeats(Math.max(0L, c.getMaxCapacity() - enrolled))
            .startTime(c.getStartTime())
            .endTime(c.getEndTime())
            .status(c.getStatus())
            .roomName(c.getRoom().getRoomName())
            .subjectName(c.getSubject().getSubjectName())
            .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .coachId(c.getCoach() != null ? c.getCoach().getUserId() : null)
            .coachSpeciality(c.getCoach() != null ? c.getCoach().getSpeciality() : null)
            .roomId(c.getRoom().getRoomId())
            .subjectId(c.getSubject().getSubjectId())
            .subjectDescription(c.getSubject().getDescription()).build();

    }
}
