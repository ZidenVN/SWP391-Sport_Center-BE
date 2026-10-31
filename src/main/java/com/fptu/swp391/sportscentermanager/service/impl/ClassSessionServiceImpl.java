package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ClassSessionResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MakeupSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.PostponeSessionRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SubstituteCoachRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.guard.CoachSubjectGuard;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.ClassSessionService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSessionServiceImpl implements ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final SportClassRepository sportClassRepository;
    private final CoachRepository coachRepository;
    private final RoomRepository roomRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponseDTO> getSessionsByClass(Long classId) {
        if (!sportClassRepository.existsById(classId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }
        return classSessionRepository.findBySportClass_ClassIdOrderByStartTimeAsc(classId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ClassSessionResponseDTO substituteCoach(Long sessionId, SubstituteCoachRequestDTO dto) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new AppException(ErrorCode.SESSION_CANNOT_SUBSTITUTE);
        }

        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.SESSION_CANNOT_SUBSTITUTE);
        }

        Coach substituteCoach = coachRepository.findById(dto.getCoachId())
                .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (substituteCoach.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }

        CoachSubjectGuard.requireTeaches(substituteCoach, session.getSportClass().getSubject());

        boolean hasConflict = classSessionRepository.existsCoachConflict(dto.getCoachId(), session.getStartTime(), session.getEndTime(), sessionId)
                || sportClassRepository.existsCoachConflict(dto.getCoachId(), session.getStartTime(), session.getEndTime(), session.getSportClass().getClassId());
        if (hasConflict) {
            throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
        }

        session.setCoach(substituteCoach);
        String coachNote = "HLV dạy thay: " + substituteCoach.getFirstName() + " " + substituteCoach.getLastName();
        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            coachNote += " (" + dto.getNote().trim() + ")";
        }
        session.setNote(session.getNote() != null ? session.getNote() + " | " + coachNote : coachNote);

        ClassSession saved = classSessionRepository.save(session);

        // Gửi thông báo cho hội viên trong lớp
        List<Member> members = registeredMembers(session.getSportClass().getClassId());
        if (!members.isEmpty()) {
            String memberMsg = "HLV " + substituteCoach.getFirstName() + " " + substituteCoach.getLastName()
                    + " sẽ dạy thay buổi học ngày " + session.getStartTime().format(TIME_FMT)
                    + " của lớp " + session.getSportClass().getSubject().getSubjectName() + ".";
            notificationService.notifyUsers(members, "Thay đổi HLV buổi học", memberMsg, "COACH_SUBSTITUTE");
        }

        // Gửi thông báo cho HLV dạy thay
        notificationService.notifyUsers(List.of(substituteCoach), "Phân công dạy thay",
                "Bạn được phân công dạy thay buổi học lớp " + session.getSportClass().getSubject().getSubjectName()
                        + " lúc " + session.getStartTime().format(TIME_FMT)
                        + " tại phòng " + session.getRoom().getRoomName() + ".", "COACH_ASSIGNED");

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClassSessionResponseDTO postponeSession(Long sessionId, PostponeSessionRequestDTO dto) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new AppException(ErrorCode.SESSION_CANNOT_POSTPONE);
        }

        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.SESSION_CANNOT_POSTPONE);
        }

        session.setStatus(SessionStatus.POSTPONED);
        String reasonNote = "Hoãn: " + dto.getReason().trim();
        session.setNote(session.getNote() != null ? session.getNote() + " | " + reasonNote : reasonNote);

        ClassSession saved = classSessionRepository.save(session);

        // Báo cho học viên trong lớp
        List<Member> members = registeredMembers(session.getSportClass().getClassId());
        if (!members.isEmpty()) {
            String content = "Buổi học lúc " + session.getStartTime().format(TIME_FMT)
                    + " lớp " + session.getSportClass().getSubject().getSubjectName()
                    + " đã bị hoãn. Lý do: " + dto.getReason().trim() + ". Lịch học bù sẽ được thông báo sau.";
            notificationService.notifyUsers(members, "Hoãn buổi học", content, "SESSION_POSTPONED");
        }

        // Báo cho HLV phụ trách
        if (session.getCoach() != null) {
            notificationService.notifyUsers(List.of(session.getCoach()), "Buổi học bị hoãn",
                    "Buổi học lúc " + session.getStartTime().format(TIME_FMT) + " của bạn đã được hoãn.", "SESSION_POSTPONED");
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClassSessionResponseDTO createMakeupSession(Long classId, MakeupSessionRequestDTO dto) {
        SportClass sportClass = sportClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        ClassSession postponedSession = classSessionRepository.findById(dto.getPostponedSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!postponedSession.getSportClass().getClassId().equals(classId)
                || postponedSession.getStatus() != SessionStatus.POSTPONED) {
            throw new AppException(ErrorCode.MAKEUP_SESSION_INVALID);
        }

        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getEndTime().isAfter(dto.getStartTime())
                || !dto.getStartTime().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_CLASS_TIME);
        }

        // Xác định phòng học
        Room room = dto.getRoomId() != null
                ? roomRepository.findByRoomIdAndStatusNot(dto.getRoomId(), "INACTIVE").orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND))
                : postponedSession.getRoom();

        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        if (sportClass.getMaxCapacity() != null && sportClass.getMaxCapacity() > room.getCapacity()) {
            throw new AppException(ErrorCode.CLASS_CAPACITY_EXCEEDS_ROOM);
        }
        if (classSessionRepository.existsRoomConflict(room.getRoomId(), dto.getStartTime(), dto.getEndTime(), null)
                || sportClassRepository.existsRoomConflict(room.getRoomId(), dto.getStartTime(), dto.getEndTime(), 0L)) {
            throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
        }

        // Xác định HLV
        Coach coach = dto.getCoachId() != null
                ? coachRepository.findById(dto.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND))
                : postponedSession.getCoach();

        if (coach.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }
        CoachSubjectGuard.requireTeaches(coach, sportClass.getSubject());

        if (classSessionRepository.existsCoachConflict(coach.getUserId(), dto.getStartTime(), dto.getEndTime(), null)
                || sportClassRepository.existsCoachConflict(coach.getUserId(), dto.getStartTime(), dto.getEndTime(), 0L)) {
            throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
        }

        ClassSession makeupSession = ClassSession.builder()
                .sportClass(sportClass)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .coach(coach)
                .room(room)
                .type(SessionType.MAKEUP)
                .status(SessionStatus.SCHEDULED)
                .makeupFor(postponedSession)
                .note(dto.getNote() != null && !dto.getNote().isBlank()
                        ? dto.getNote().trim()
                        : "Buổi học bù cho buổi hoãn ngày " + postponedSession.getStartTime().format(TIME_FMT))
                .build();

        // Tự động kéo dài hạn kết thúc khoá học nếu buổi học bù vượt ra ngoài ngày kết thúc cũ
        boolean classUpdated = false;
        if (sportClass.getEndDate() != null && dto.getEndTime().toLocalDate().isAfter(sportClass.getEndDate())) {
            sportClass.setEndDate(dto.getEndTime().toLocalDate());
            classUpdated = true;
        }
        if (sportClass.getEndTime() != null && dto.getEndTime().isAfter(sportClass.getEndTime())) {
            sportClass.setEndTime(dto.getEndTime());
            classUpdated = true;
        }
        if (classUpdated) {
            sportClassRepository.save(sportClass);
        }

        ClassSession saved = classSessionRepository.save(makeupSession);

        // Báo cho học viên trong lớp
        List<Member> members = registeredMembers(classId);
        if (!members.isEmpty()) {
            String content = "Lịch học bù của lớp " + sportClass.getSubject().getSubjectName()
                    + " đã được sắp xếp vào lúc " + saved.getStartTime().format(TIME_FMT)
                    + " tại phòng " + room.getRoomName() + ".";
            notificationService.notifyUsers(members, "Lịch học bù mới", content, "MAKEUP_SESSION_SCHEDULED");
        }

        // Báo cho HLV phụ trách buổi bù
        notificationService.notifyUsers(List.of(coach), "Lịch dạy bù mới",
                "Bạn có lịch dạy bù lớp " + sportClass.getSubject().getSubjectName()
                        + " lúc " + saved.getStartTime().format(TIME_FMT)
                        + " tại phòng " + room.getRoomName() + ".", "MAKEUP_SESSION_SCHEDULED");

        return toResponseDTO(saved);
    }

    private List<Member> registeredMembers(Long classId) {
        return classRegistrationRepository.findActiveByClassId(classId).stream()
                .map(ClassRegistration::getMember)
                .toList();
    }

    private ClassSessionResponseDTO toResponseDTO(ClassSession s) {
        return ClassSessionResponseDTO.builder()
                .sessionId(s.getSessionId())
                .classId(s.getSportClass() != null ? s.getSportClass().getClassId() : null)
                .className(s.getSportClass() != null && s.getSportClass().getSubject() != null
                        ? s.getSportClass().getSubject().getSubjectName() : null)
                .subjectId(s.getSportClass() != null && s.getSportClass().getSubject() != null
                        ? s.getSportClass().getSubject().getSubjectId() : null)
                .subjectName(s.getSportClass() != null && s.getSportClass().getSubject() != null
                        ? s.getSportClass().getSubject().getSubjectName() : null)
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .coachId(s.getCoach() != null ? s.getCoach().getUserId() : null)
                .coachName(s.getCoach() != null ? s.getCoach().getFirstName() + " " + s.getCoach().getLastName() : "Chưa có HLV")
                .roomId(s.getRoom() != null ? s.getRoom().getRoomId() : null)
                .roomName(s.getRoom() != null ? s.getRoom().getRoomName() : null)
                .type(s.getType())
                .status(s.getStatus())
                .makeupForSessionId(s.getMakeupFor() != null ? s.getMakeupFor().getSessionId() : null)
                .note(s.getNote())
                .build();
    }
}
