package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import java.time.LocalDate;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SportClassRepository sportClassRepository;
    private final ClassAttendanceRepository classAttendanceRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final CoachScheduleRepository coachScheduleRepository;
    private final SubjectRepository subjectRepository;
    @Value("${app.attendance.open-minutes-before}")
    private long attendanceOpenMinutesBefore;

    @Value("${app.attendance.close-hours-after}")
    private long attendanceCloseHoursAfter;

    // Chỉ điểm danh / sửa điểm danh từ 15 phút trước giờ học đến 24 giờ sau khi BUỔI kết thúc.
    // Buổi bị huỷ / hoãn, hoặc lớp bị huỷ / xoá mềm thì không điểm danh.
    private void ensureAttendanceWindow(ClassSession session, LocalDateTime now) {
        SessionStatus sessionStatus = session.getStatus();
        String classStatus = session.getSportClass().getStatus();
        if (sessionStatus == SessionStatus.CANCELLED || sessionStatus == SessionStatus.POSTPONED
            || "CANCELLED".equals(classStatus) || "INACTIVE".equals(classStatus)) {
            throw new AppException(ErrorCode.ATTENDANCE_CLASS_NOT_ACTIVE);
        }

        if (now.isBefore(session.getStartTime().minusMinutes(attendanceOpenMinutesBefore))) {
            throw new AppException(ErrorCode.ATTENDANCE_TOO_EARLY);
        }

        if (now.isAfter(session.getEndTime().plusHours(attendanceCloseHoursAfter))) {
            throw new AppException(ErrorCode.ATTENDANCE_CLOSED);
        }
    }

    // Buổi phải tồn tại và do chính HLV này dạy (HLV chính bị thay thì không còn quyền với buổi đó)
    private ClassSession getTaughtSessionOrThrow(Long coachId, Long sessionId) {
        ClassSession session = coachScheduleRepository.findDetailById(sessionId)
            .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private List<ClassRosterResponseDTO> toRoster(List<ClassRegistration> registrations) {
        List<ClassRosterResponseDTO> result = new ArrayList<>();
        for (ClassRegistration r : registrations) {
            Member m = r.getMember();
            result.add(ClassRosterResponseDTO.builder()
                .memberId(m.getUserId())
                .firstName(m.getFirstName())
                .lastName(m.getLastName())
                .email(m.getEmail())
                .phone(m.getPhone())
                .gender(m.getGender())
                .trainingGoal(m.getTrainingGoal())
                .enrolledDate(r.getRegisteredAt())
                .enrollmentStatus(r.getStatus())
                .build());
        }
        return result;
    }

    private SportClass getOwnedClassOrThrow(Long coachId, Long classId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }
        return sportClass;
    }

    // Tải các môn ACTIVE theo id; id nào không có thì báo SUBJECT_NOT_FOUND
    private Set<Subject> loadActiveSubjects(Set<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Set<Subject> result = new HashSet<>();
        for (Long subjectId : subjectIds) {
            result.add(subjectRepository.findBySubjectIdAndStatus(subjectId, "ACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND)));
        }
        return result;
    }

    // Cột speciality cũ vẫn NOT NULL trong DB, nên ghi tên các môn vào đó
    private String joinNames(Set<Subject> subjects) {
        return subjects.stream().map(Subject::getSubjectName).sorted().collect(Collectors.joining(", "));
    }

    private CoachResponseDTO toCoachResponse(Coach c) {
        return CoachResponseDTO.builder()
            .coachId(c.getUserId())
            .firstName(c.getFirstName())
            .lastName(c.getLastName())
            .gender(c.getGender())
            .email(c.getEmail())
            .phone(c.getPhone())
            .speciality(c.getSpeciality())
            .subjects(c.getSubjects().stream()
                .map(s -> CoachSubjectDTO.builder().subjectId(s.getSubjectId()).subjectName(s.getSubjectName()).build())
                .toList())
            .status(c.getStatus() != null ? c.getStatus().name() : null)
            .build();
    }

    private AttendanceResponseDTO toAttendanceResponse(ClassAttendance a) {
        Member m = a.getMember();
        return AttendanceResponseDTO.builder()
            .attendanceId(a.getAttendanceId())
            .memberId(m.getUserId())
            .memberName(m.getFirstName() + " " + m.getLastName())
            .phone(m.getPhone())
            .attendanceDate(a.getAttendanceDate())
            .status(a.getStatus())
            .note(a.getNote())
            .markedAt(a.getMarkedAt())
            .build();
    }

    @Override
    @Transactional
    public CoachResponseDTO createCoach(CoachRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (requestDTO.getPhone() != null && userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        Role coachRole = roleRepository.findByRoleName("COACH");
        if (coachRole == null) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        Set<Subject> subjects = loadActiveSubjects(requestDTO.getSubjectIds());

        Coach newCoach = Coach.builder()
            .firstName(requestDTO.getFirstName())
            .lastName(requestDTO.getLastName())
            .gender(requestDTO.getGender())
            .phone(requestDTO.getPhone())
            .email(requestDTO.getEmail())
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .status(UserStatus.ACTIVE)
            .role(coachRole)
            .speciality(joinNames(subjects))
            .subjects(subjects)
            .build();

        Coach savedCoach = coachRepository.save(newCoach);
        return toCoachResponse(savedCoach);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachResponseDTO> getAllCoaches() {
        return coachRepository.findAll().stream().map(this::toCoachResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CoachResponseDTO getCoachById(Long id) {
        // BE-27: vẫn trả HLV đã khóa (kèm status) để Manager xem và mở khóa lại
        Coach c = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        return toCoachResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachResponseDTO> getCoachesBySubject(Long subjectId) {
        return coachRepository.findDistinctBySubjects_SubjectId(subjectId).stream()
            .filter(c -> c.getStatus() == UserStatus.ACTIVE)
            .map(this::toCoachResponse)
            .toList();
    }

    @Override
    @Transactional
    public CoachResponseDTO updateCoach(Long id, CoachUpdateRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (!requestDTO.getEmail().equalsIgnoreCase(coach.getEmail()) && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        if (!requestDTO.getPhone().equals(coach.getPhone()) && userRepository.existsByPhone(requestDTO.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_TAKEN);
        }

        coach.setFirstName(requestDTO.getFirstName());
        coach.setLastName(requestDTO.getLastName());
        coach.setGender(requestDTO.getGender());
        coach.setPhone(requestDTO.getPhone());
        coach.setEmail(requestDTO.getEmail());
        Set<Subject> newSubjects = loadActiveSubjects(requestDTO.getSubjectIds());
        // Môn bị bỏ mà HLV còn lớp chưa kết thúc của môn đó thì chặn
        Set<Long> removedIds = coach.getSubjects().stream()
            .map(Subject::getSubjectId)
            .filter(sid -> newSubjects.stream().noneMatch(s -> s.getSubjectId().equals(sid)))
            .collect(Collectors.toSet());
        if (!removedIds.isEmpty() && coachRepository.hasOpeningClassOfSubjects(id, removedIds)) {
            throw new AppException(ErrorCode.COACH_SUBJECT_IN_USE);
        }
        coach.getSubjects().clear();
        coach.getSubjects().addAll(newSubjects);
        coach.setSpeciality(joinNames(newSubjects));

        Coach savedCoach = coachRepository.save(coach);

        return toCoachResponse(savedCoach);
    }

    @Override
    @Transactional
    public void toggleCoachStatus(Long id) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (UserStatus.ACTIVE == coach.getStatus()) {
            // BE-27: không khóa HLV khi còn lớp sắp dạy, tránh lớp mồ côi
            if (sportClassRepository.existsByCoach_UserIdAndStatusAndStartTimeAfter(id, "OPENING", LocalDateTime.now())) {
                throw new AppException(ErrorCode.COACH_HAS_UPCOMING_CLASSES);
            }
            coach.setStatus(UserStatus.INACTIVE);
        } else {
            coach.setStatus(UserStatus.ACTIVE);
        }
        coachRepository.save(coach);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingScheduleResponseDTO> getMyTeachingSchedule(Long coachId, LocalDate from, LocalDate to) {
        LocalDate fromDate = (from != null) ? from : LocalDate.now().minusDays(1);
        LocalDate toDate = (to != null) ? to : LocalDate.now().plusDays(30);
        if (toDate.isBefore(fromDate)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<ClassSession> sessions = coachScheduleRepository.findTeachingSessions(
            coachId, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        // 1 lớp có nhiều buổi: đếm sĩ số mỗi lớp 1 lần
        Map<Long, Long> enrolledByClass = new HashMap<>();
        List<TeachingScheduleResponseDTO> result = new ArrayList<>();
        for (ClassSession s : sessions) {
            SportClass c = s.getSportClass();
            long enrolled = enrolledByClass.computeIfAbsent(c.getClassId(),
                id -> classRegistrationRepository.countBySportClass_ClassIdAndStatus(id, "REGISTERED"));
            Coach mainCoach = c.getCoach();

            result.add(TeachingScheduleResponseDTO.builder()
                .sessionId(s.getSessionId())
                .classId(c.getClassId())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : "Chưa có môn nào")
                .roomName(s.getRoom() != null ? s.getRoom().getRoomName() : "Chưa có phòng")
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .maxCapacity(c.getMaxCapacity())
                .currentEnrolled(enrolled)
                .status(s.getStatus().name())
                .classStatus(c.getStatus())
                .type(s.getType().name())
                .substitute(mainCoach != null && !mainCoach.getUserId().equals(coachId))
                .mainCoachName(mainCoach != null ? mainCoach.getLastName() + " " + mainCoach.getFirstName() : null)
                .imageUrl(c.getImageUrl())
                .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRosterResponseDTO> getClassRoster(Long coachId, Long classId) {
        getOwnedClassOrThrow(coachId, classId);
        return toRoster(classRegistrationRepository.findActiveByClassId(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRosterResponseDTO> getSessionRoster(Long coachId, Long sessionId) {
        ClassSession session = getTaughtSessionOrThrow(coachId, sessionId);
        return toRoster(classRegistrationRepository.findActiveByClassId(session.getSportClass().getClassId()));
    }

    @Override
    @Transactional
    public List<AttendanceResponseDTO> takeAttendance(Long coachId, Long sessionId, TakeAttendanceRequestDTO requestDTO) {
        // buổi phải tồn tại và do HLV này dạy
        ClassSession session = getTaughtSessionOrThrow(coachId, sessionId);
        LocalDateTime now = LocalDateTime.now();
        ensureAttendanceWindow(session, now);

        if (requestDTO.getRecords() == null || requestDTO.getRecords().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // học viên của buổi = học viên đang học của lớp chứa buổi đó
        Map<Long, Member> enrolledMembers = new HashMap<>();
        for (ClassRegistration r : classRegistrationRepository.findActiveByClassId(session.getSportClass().getClassId())) {
            enrolledMembers.put(r.getMember().getUserId(), r.getMember());
        }

        Map<Long, ClassAttendance> existing = new HashMap<>();
        for (ClassAttendance a : classAttendanceRepository.findByClassSession_SessionId(sessionId)) {
            existing.put(a.getMember().getUserId(), a);
        }

        Set<Long> seen = new HashSet<>();
        List<ClassAttendance> toSave = new ArrayList<>();
        for (AttendanceRecordDTO record : requestDTO.getRecords()) {
            Long memberId = record.getMemberId();
            Member member = enrolledMembers.get(memberId);
            if (member == null)
                throw new AppException(ErrorCode.MEMBER_NOT_IN_CLASS);
            if (!seen.add(memberId))
                throw new AppException(ErrorCode.DUPLICATE_ATTENDANCE);

            ClassAttendance attendance = existing.getOrDefault(memberId, ClassAttendance.builder()
                .classSession(session)
                .member(member)
                .attendanceDate(session.getStartTime())
                .build());
            attendance.setStatus(record.getStatus());
            attendance.setNote(record.getNote());
            attendance.setMarkedAt(now);
            toSave.add(attendance);
        }
        return classAttendanceRepository.saveAll(toSave).stream().map(this::toAttendanceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getSessionAttendance(Long coachId, Long sessionId) {
        getTaughtSessionOrThrow(coachId, sessionId);
        return classAttendanceRepository.findByClassSession_SessionId(sessionId).stream().map(this::toAttendanceResponse).toList();
    }

}

