package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.ClassRegistration;
import com.fptu.swp391.sportscentermanager.entity.Coach;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.Room;
import com.fptu.swp391.sportscentermanager.entity.SportClass;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.ClassStatus;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassRegistrationRepository;
import com.fptu.swp391.sportscentermanager.repository.CoachRepository;
import com.fptu.swp391.sportscentermanager.repository.RoomRepository;
import com.fptu.swp391.sportscentermanager.repository.SportClassRepository;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.RoomService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SportClassServiceImpl implements SportClassService {
    private final SportClassRepository sportClassRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final CoachRepository coachRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final NotificationService notificationService;

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public SportClassResponseDTO createClass(SportClassRequestDTO requestDTO) {
        Room room = roomRepository.findById(requestDTO.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

       Subject subject = subjectRepository.findById(requestDTO.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

       // TODO: Xử lý logic gán Coach sau khi hoàn thiện module Coach
       Coach coach = null;
       if (requestDTO.getCoachId() != null) {
           coach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
       }

        SportClass newClass = SportClass.builder().maxCapacity(requestDTO.getMaxCapacity()).scheduleTime(requestDTO.getScheduleTime()).status("OPENING").room(room).subject(subject).coach(coach).build();

        SportClass savedClass = sportClassRepository.save(newClass);

        return SportClassResponseDTO.builder().classId(savedClass.getClassId()).maxCapacity(savedClass.getMaxCapacity()).scheduleTime(savedClass.getScheduleTime()).status(savedClass.getStatus()).roomName(savedClass.getRoom().getRoomName()).subjectName(savedClass.getSubject().getSubjectName()).coachName(savedClass.getCoach() != null ? savedClass.getCoach().getFirstName() + " " + savedClass.getCoach().getLastName() : "Chưa có HLV").build();
    }

    @Override
    public SportClass getClassById(Long id) {
        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    @Override
    public SportClassResponseDTO getSportClassById(Long id) {
        SportClass c = getClassById(id);
        return SportClassResponseDTO.builder()
            .classId(c.getClassId())
            .maxCapacity(c.getMaxCapacity())
            .scheduleTime(c.getScheduleTime())
            .status(c.getStatus())
            .roomName(c.getRoom().getRoomName())
            .subjectName(c.getSubject().getSubjectName())
            .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .build();
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
                .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV").build();
            result.add(dto);
        }
        return result;
    }

    @Override
    public SportClassResponseDTO assignCoachToClass(Long classId, Long coachId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        requireOpening(sportClass);

        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (coach.getStatus() != UserStatus.ACTIVE){
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }

        sportClass.setCoach(coach);
        SportClass savedClass = sportClassRepository.save(sportClass);

        return SportClassResponseDTO.builder()
            .classId(savedClass.getClassId())
            .maxCapacity(savedClass.getMaxCapacity())
            .scheduleTime(savedClass.getScheduleTime())
            .status(savedClass.getStatus())
            .roomName(savedClass.getRoom().getRoomName())
            .subjectName(savedClass.getSubject().getSubjectName())
            .coachName(savedClass.getCoach() != null ? savedClass.getCoach().getFirstName() + " " + savedClass.getCoach().getLastName() : "Chưa có HLV")
            .build();
    }

    @Override
    public SportClassResponseDTO updateClass(Long id, SportClassRequestDTO requestDTO) {
        SportClass sportClass = sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        requireOpening(sportClass);

        // BE-16: giữ lại giờ/phòng cũ để biết có cần báo học viên không
        LocalDateTime oldTime = sportClass.getScheduleTime();
        Long oldRoomId = sportClass.getRoom() != null ? sportClass.getRoom().getRoomId() : null;

        sportClass.setMaxCapacity(requestDTO.getMaxCapacity());
        sportClass.setScheduleTime(requestDTO.getScheduleTime());

        if (!sportClass.getRoom().getRoomId().equals(requestDTO.getRoomId())){
            Room newRoom = roomRepository.findById(requestDTO.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
            sportClass.setRoom(newRoom);
        }

        if (!sportClass.getSubject().getSubjectId().equals(requestDTO.getSubjectId())){
            Subject newSubject = subjectRepository.findById(requestDTO.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
            sportClass.setSubject(newSubject);
        }

        if (requestDTO.getCoachId() != null){
            Coach newCoach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
            sportClass.setCoach(newCoach);
        } else {
            sportClass.setCoach(null);
        }

        SportClass savedClass = sportClassRepository.save(sportClass);

        boolean scheduleChanged = !Objects.equals(oldTime, savedClass.getScheduleTime())
            || !Objects.equals(oldRoomId, savedClass.getRoom().getRoomId());
        if (scheduleChanged) {
            notificationService.notifyUsers(registeredMembers(savedClass.getClassId()),
                "Lớp " + savedClass.getSubject().getSubjectName() + " đổi lịch",
                "Lớp học đã đổi sang " + savedClass.getScheduleTime().format(TIME_FMT)
                    + " tại phòng " + savedClass.getRoom().getRoomName() + ".",
                "SCHEDULE_CHANGE");
        }

        return getSportClassById(savedClass.getClassId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SportClassResponseDTO> searchClasses(LocalDate fromDate, LocalDate toDate, Long subjectId, Long coachId,
                                                     Long roomId, String status, Boolean available, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String statusFilter = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.ASC, "scheduleTime"));
        Page<SportClass> result = sportClassRepository.search(from, to, subjectId, roomId, coachId,
            statusFilter, available, pageable);

        // Đếm số chỗ đã đăng ký cho cả trang bằng 1 câu query
        Map<Long, Long> registered = new HashMap<>();
        List<Long> ids = result.getContent().stream().map(SportClass::getClassId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : classRegistrationRepository.countActiveByClassIds(ids)) {
                registered.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        return result.map(c -> toResponse(c, registered.getOrDefault(c.getClassId(), 0L)));
    }

    @Override
    @Transactional
    public SportClassResponseDTO cancelClass(Long id, String reason) {
        SportClass sportClass = sportClassRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        requireOpening(sportClass);

        // Hủy luôn các đăng ký đang có để không còn chiếm chỗ / hiện trong lịch sắp tới
        List<ClassRegistration> active = classRegistrationRepository.findActiveByClassId(id);
        List<Member> members = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ClassRegistration r : active) {
            r.setStatus("CANCELLED");
            r.setCancelledAt(now);
            members.add(r.getMember());
        }
        classRegistrationRepository.saveAll(active);

        sportClass.setStatus(ClassStatus.CANCELLED.name());
        sportClassRepository.save(sportClass);

        String content = "Lớp " + sportClass.getSubject().getSubjectName() + " lúc "
            + sportClass.getScheduleTime().format(TIME_FMT) + " đã bị hủy.";
        if (reason != null && !reason.isBlank()) {
            content += " Lý do: " + reason.trim();
        }
        notificationService.notifyUsers(members, "Lớp học bị hủy", content, "CLASS_CANCELLED");

        return toResponse(sportClass, 0L);
    }

    @Override
    @Transactional
    public SportClassResponseDTO completeClass(Long id) {
        SportClass sportClass = getClassById(id);
        requireOpening(sportClass);
        if (sportClass.getScheduleTime() == null || sportClass.getScheduleTime().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CLASS_NOT_STARTED);
        }
        sportClass.setStatus(ClassStatus.COMPLETED.name());
        sportClassRepository.save(sportClass);
        return toResponse(sportClass,
            classRegistrationRepository.countBySportClass_ClassIdAndStatus(id, "REGISTERED"));
    }

    private void requireOpening(SportClass sportClass) {
        if (!ClassStatus.OPENING.name().equals(sportClass.getStatus())) {
            throw new AppException(ErrorCode.CLASS_ALREADY_CLOSED);
        }
    }

    private List<Member> registeredMembers(Long classId) {
        return classRegistrationRepository.findActiveByClassId(classId).stream()
            .map(ClassRegistration::getMember).toList();
    }

    private SportClassResponseDTO toResponse(SportClass c, long registeredCount) {
        int max = c.getMaxCapacity() != null ? c.getMaxCapacity() : 0;
        return SportClassResponseDTO.builder()
            .classId(c.getClassId())
            .maxCapacity(c.getMaxCapacity())
            .scheduleTime(c.getScheduleTime())
            .status(c.getStatus())
            .roomId(c.getRoom() != null ? c.getRoom().getRoomId() : null)
            .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
            .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
            .coachId(c.getCoach() != null ? c.getCoach().getUserId() : null)
            .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .registeredCount(registeredCount)
            .availableSeats(Math.max(0, max - (int) registeredCount))
            .build();
    }

    @Override
    public void deleteClass(Long id) {
        SportClass sportClass = getClassById(id);
        sportClass.setStatus("INACTIVE");
        sportClassRepository.delete(sportClass);
    }
}
