package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.SportClassRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.SportClassResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ClassStatus;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.SessionStatus;
import com.fptu.swp391.sportscentermanager.enums.SessionType;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.guard.CoachSubjectGuard;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.SportClassService;
import com.fptu.swp391.sportscentermanager.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
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
    private final ImageStorageService imageStorageService;
    private final SubscriptionService subscriptionService;
    private final ClassSessionRepository classSessionRepository;

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    @Transactional
    public SportClassResponseDTO createClass(SportClassRequestDTO requestDTO, MultipartFile image) {
        Room room = roomRepository.findByRoomIdAndStatusNot(requestDTO.getRoomId(), "INACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        validateRoomForClass(room, requestDTO.getMaxCapacity());

        Subject subject = subjectRepository.findBySubjectIdAndStatus(requestDTO.getSubjectId(), "ACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        Coach coach = null;
        if (requestDTO.getCoachId() != null) {
            coach = coachRepository.findById(requestDTO.getCoachId())
                    .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
            if (coach.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.COACH_INACTIVE);
            }
            CoachSubjectGuard.requireTeaches(coach, subject);
        }

        String imageUrl = imageStorageService.uploadClassImage(image);

        // Phân biệt chế độ tạo lớp lặp lịch cố định hay lớp đơn lẻ (tương thích ngược)
        if (requestDTO.getStartDate() != null && requestDTO.getDurationDays() != null && requestDTO.getDurationDays() > 0) {
            if (requestDTO.getStartDate().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.CLASS_START_DATE_INVALID);
            }
            if (requestDTO.getStartClock() == null || requestDTO.getEndClock() == null
                    || !requestDTO.getEndClock().isAfter(requestDTO.getStartClock())) {
                throw new AppException(ErrorCode.INVALID_CLASS_TIME);
            }

            Set<DayOfWeek> days = parseDaysOfWeek(requestDTO.getDaysOfWeek());
            LocalDate endDate = requestDTO.getStartDate().plusDays(requestDTO.getDurationDays());

            List<ClassSession> sessionsToCreate = new ArrayList<>();
            for (LocalDate d = requestDTO.getStartDate(); !d.isAfter(endDate); d = d.plusDays(1)) {
                if (days.contains(d.getDayOfWeek())) {
                    LocalDateTime sStart = d.atTime(requestDTO.getStartClock());
                    LocalDateTime sEnd = d.atTime(requestDTO.getEndClock());

                    if (classSessionRepository.existsRoomConflict(room.getRoomId(), sStart, sEnd, null)
                            || sportClassRepository.existsRoomConflict(room.getRoomId(), sStart, sEnd, 0L)) {
                        throw new AppException(ErrorCode.SESSION_SCHEDULE_CONFLICT);
                    }
                    if (coach != null && (classSessionRepository.existsCoachConflict(coach.getUserId(), sStart, sEnd, null)
                            || sportClassRepository.existsCoachConflict(coach.getUserId(), sStart, sEnd, 0L))) {
                        throw new AppException(ErrorCode.SESSION_SCHEDULE_CONFLICT);
                    }

                    sessionsToCreate.add(ClassSession.builder()
                            .startTime(sStart)
                            .endTime(sEnd)
                            .coach(coach)
                            .room(room)
                            .type(SessionType.REGULAR)
                            .status(SessionStatus.SCHEDULED)
                            .build());
                }
            }

            if (sessionsToCreate.isEmpty()) {
                throw new AppException(ErrorCode.CLASS_DAYS_OF_WEEK_INVALID);
            }

            LocalDateTime firstSessionAt = sessionsToCreate.get(0).getStartTime();
            LocalDateTime lastSessionEnd = sessionsToCreate.get(sessionsToCreate.size() - 1).getEndTime();

            SportClass newClass = SportClass.builder()
                    .startDate(requestDTO.getStartDate())
                    .endDate(endDate)
                    .durationDays(requestDTO.getDurationDays())
                    .daysOfWeek(requestDTO.getDaysOfWeek())
                    .startClock(requestDTO.getStartClock())
                    .endClock(requestDTO.getEndClock())
                    .firstSessionAt(firstSessionAt)
                    .startTime(firstSessionAt)
                    .endTime(lastSessionEnd)
                    .maxCapacity(requestDTO.getMaxCapacity())
                    .status(ClassStatus.OPENING.name())
                    .room(room)
                    .subject(subject)
                    .coach(coach)
                    .imageUrl(imageUrl)
                    .build();

            SportClass savedClass = sportClassRepository.save(newClass);
            for (ClassSession s : sessionsToCreate) {
                s.setSportClass(savedClass);
            }
            classSessionRepository.saveAll(sessionsToCreate);

            return toResponseDTO(savedClass);
        } else {
            // Chế độ tương thích ngược
            validateClassTime(requestDTO.getStartTime(), requestDTO.getEndTime());
            checkScheduleConflict(requestDTO.getRoomId(), requestDTO.getCoachId(), requestDTO.getStartTime(), requestDTO.getEndTime(), 0L);

            SportClass newClass = SportClass.builder()
                    .startDate(requestDTO.getStartTime().toLocalDate())
                    .endDate(requestDTO.getEndTime().toLocalDate())
                    .durationDays(1)
                    .daysOfWeek(requestDTO.getStartTime().getDayOfWeek().name())
                    .startClock(requestDTO.getStartTime().toLocalTime())
                    .endClock(requestDTO.getEndTime().toLocalTime())
                    .firstSessionAt(requestDTO.getStartTime())
                    .startTime(requestDTO.getStartTime())
                    .endTime(requestDTO.getEndTime())
                    .maxCapacity(requestDTO.getMaxCapacity())
                    .status(ClassStatus.OPENING.name())
                    .room(room)
                    .subject(subject)
                    .coach(coach)
                    .imageUrl(imageUrl)
                    .build();

            SportClass savedClass = sportClassRepository.save(newClass);

            ClassSession session = ClassSession.builder()
                    .sportClass(savedClass)
                    .startTime(requestDTO.getStartTime())
                    .endTime(requestDTO.getEndTime())
                    .coach(coach)
                    .room(room)
                    .type(SessionType.REGULAR)
                    .status(SessionStatus.SCHEDULED)
                    .build();
            classSessionRepository.save(session);

            return toResponseDTO(savedClass);
        }
    }

    @Override
    public SportClass getClassById(Long id) {
        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    @Override
    public SportClassResponseDTO getSportClassById(Long id) {
        return toResponseDTO(getClassById(id));
    }

    @Override
    public List<SportClassResponseDTO> getAllClasses() {
        return sportClassRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public SportClassResponseDTO assignCoachToClass(Long classId, Long coachId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        requireOpening(sportClass);

        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (coach.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.COACH_INACTIVE);
        }

        CoachSubjectGuard.requireTeaches(coach, sportClass.getSubject());

        // Kiểm tra xung đột lịch cho toàn bộ các buổi học sắp tới của lớp
        List<ClassSession> futureSessions = classSessionRepository.findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(classId, LocalDateTime.now());
        for (ClassSession s : futureSessions) {
            if (s.getStatus() == SessionStatus.SCHEDULED) {
                if (classSessionRepository.existsCoachConflict(coachId, s.getStartTime(), s.getEndTime(), s.getSessionId())) {
                    throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
                }
            }
        }

        // Cập nhật HLV cho các buổi tương lai
        for (ClassSession s : futureSessions) {
            if (s.getStatus() == SessionStatus.SCHEDULED) {
                s.setCoach(coach);
            }
        }
        classSessionRepository.saveAll(futureSessions);

        sportClass.setCoach(coach);
        SportClass savedClass = sportClassRepository.save(sportClass);

        // Báo cho HLV và học viên
        notificationService.notifyUsers(List.of(coach), "Phân công phụ trách lớp",
                "Bạn được phân công phụ trách lớp " + sportClass.getSubject().getSubjectName() + ".", "COACH_ASSIGNED");

        List<Member> members = registeredMembers(classId);
        if (!members.isEmpty()) {
            notificationService.notifyUsers(members, "Thay đổi HLV phụ trách",
                    "Lớp " + sportClass.getSubject().getSubjectName() + " hiện do HLV "
                            + coach.getFirstName() + " " + coach.getLastName() + " phụ trách.", "CLASS_UPDATED");
        }

        return toResponseDTO(savedClass);
    }

    @Override
    @Transactional
    public SportClassResponseDTO updateClass(Long id, SportClassRequestDTO requestDTO, MultipartFile image) {
        SportClass sportClass = sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        requireOpening(sportClass);

        long registered = classRegistrationRepository.countBySportClass_ClassIdAndStatus(id, "REGISTERED");
        LocalDateTime firstSession = sportClass.getFirstSessionAt() != null ? sportClass.getFirstSessionAt() : sportClass.getStartTime();
        boolean hasStarted = firstSession != null && !LocalDateTime.now().isBefore(firstSession);
        boolean scheduleChanged = isScheduleChanged(sportClass, requestDTO);

        // CASE 3: Lớp đã bắt đầu -> chỉ cho phép sửa sĩ số và ảnh
        if (hasStarted) {
            boolean coachDiffers = requestDTO.getCoachId() != null
                    && !Objects.equals(requestDTO.getCoachId(), sportClass.getCoach() != null ? sportClass.getCoach().getUserId() : null);
            boolean roomDiffers = requestDTO.getRoomId() != null
                    && !Objects.equals(requestDTO.getRoomId(), sportClass.getRoom() != null ? sportClass.getRoom().getRoomId() : null);
            boolean subjectDiffers = requestDTO.getSubjectId() != null
                    && !Objects.equals(requestDTO.getSubjectId(), sportClass.getSubject() != null ? sportClass.getSubject().getSubjectId() : null);

            if (scheduleChanged || coachDiffers || roomDiffers || subjectDiffers) {
                throw new AppException(ErrorCode.CLASS_CANNOT_MODIFY_STARTED);
            }

            if (requestDTO.getMaxCapacity() < registered) {
                throw new AppException(ErrorCode.CAPACITY_BELOW_REGISTERED);
            }
            sportClass.setMaxCapacity(requestDTO.getMaxCapacity());
            if (image != null && !image.isEmpty()) {
                sportClass.setImageUrl(imageStorageService.uploadClassImage(image));
            }
            SportClass saved = sportClassRepository.save(sportClass);
            return toResponseDTO(saved);
        }

        // CASE 2: Chưa bắt đầu nhưng đã có người đăng ký -> không được sửa lịch học, được sửa HLV, phòng, sĩ số, ảnh
        if (registered > 0) {
            if (scheduleChanged) {
                throw new AppException(ErrorCode.CLASS_SCHEDULE_CANNOT_MODIFY);
            }
            if (!Objects.equals(requestDTO.getSubjectId(), sportClass.getSubject().getSubjectId())) {
                throw new AppException(ErrorCode.CLASS_SCHEDULE_CANNOT_MODIFY);
            }

            if (requestDTO.getMaxCapacity() < registered) {
                throw new AppException(ErrorCode.CAPACITY_BELOW_REGISTERED);
            }
            sportClass.setMaxCapacity(requestDTO.getMaxCapacity());

            Room oldRoom = sportClass.getRoom();
            Room newRoom = oldRoom;
            if (!Objects.equals(requestDTO.getRoomId(), oldRoom.getRoomId())) {
                newRoom = roomRepository.findByRoomIdAndStatusNot(requestDTO.getRoomId(), "INACTIVE")
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
                validateRoomForClass(newRoom, requestDTO.getMaxCapacity());

                List<ClassSession> futureSessions = classSessionRepository.findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(id, LocalDateTime.now());
                for (ClassSession s : futureSessions) {
                    if (s.getStatus() == SessionStatus.SCHEDULED) {
                        if (classSessionRepository.existsRoomConflict(newRoom.getRoomId(), s.getStartTime(), s.getEndTime(), s.getSessionId())) {
                            throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
                        }
                    }
                }
                for (ClassSession s : futureSessions) {
                    if (s.getStatus() == SessionStatus.SCHEDULED) {
                        s.setRoom(newRoom);
                    }
                }
                classSessionRepository.saveAll(futureSessions);
                sportClass.setRoom(newRoom);
            }

            Coach oldCoach = sportClass.getCoach();
            Coach newCoach = oldCoach;
            if (!Objects.equals(requestDTO.getCoachId(), oldCoach != null ? oldCoach.getUserId() : null)) {
                if (requestDTO.getCoachId() != null) {
                    newCoach = coachRepository.findById(requestDTO.getCoachId())
                            .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
                    if (newCoach.getStatus() != UserStatus.ACTIVE) {
                        throw new AppException(ErrorCode.COACH_INACTIVE);
                    }
                    CoachSubjectGuard.requireTeaches(newCoach, sportClass.getSubject());

                    List<ClassSession> futureSessions = classSessionRepository.findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(id, LocalDateTime.now());
                    for (ClassSession s : futureSessions) {
                        if (s.getStatus() == SessionStatus.SCHEDULED) {
                            if (classSessionRepository.existsCoachConflict(newCoach.getUserId(), s.getStartTime(), s.getEndTime(), s.getSessionId())) {
                                throw new AppException(ErrorCode.COACH_SCHEDULE_CONFLICT);
                            }
                        }
                    }
                    for (ClassSession s : futureSessions) {
                        if (s.getStatus() == SessionStatus.SCHEDULED) {
                            s.setCoach(newCoach);
                        }
                    }
                    classSessionRepository.saveAll(futureSessions);
                } else {
                    newCoach = null;
                }
                sportClass.setCoach(newCoach);
            }

            if (image != null && !image.isEmpty()) {
                sportClass.setImageUrl(imageStorageService.uploadClassImage(image));
            }

            SportClass saved = sportClassRepository.save(sportClass);

            boolean roomChanged = !Objects.equals(oldRoom.getRoomId(), newRoom.getRoomId());
            boolean coachChanged = !Objects.equals(oldCoach != null ? oldCoach.getUserId() : null, newCoach != null ? newCoach.getUserId() : null);
            if (roomChanged || coachChanged) {
                List<Member> members = registeredMembers(id);
                if (!members.isEmpty()) {
                    String msg = "Lớp " + sportClass.getSubject().getSubjectName() + " đã cập nhật: "
                            + (roomChanged ? "Phòng học mới: " + newRoom.getRoomName() + ". " : "")
                            + (coachChanged ? "HLV mới: " + (newCoach != null ? newCoach.getFirstName() + " " + newCoach.getLastName() : "Chưa có HLV") : "");
                    notificationService.notifyUsers(members, "Cập nhật thông tin lớp", msg, "CLASS_UPDATED");
                }
            }

            return toResponseDTO(saved);
        }

        // CASE 1: Chưa bắt đầu và chưa có ai đăng ký (0 đăng ký) -> Được sửa toàn bộ, tái tạo buổi học nếu đổi lịch
        Room room = roomRepository.findByRoomIdAndStatusNot(requestDTO.getRoomId(), "INACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        validateRoomForClass(room, requestDTO.getMaxCapacity());

        Subject subject = subjectRepository.findBySubjectIdAndStatus(requestDTO.getSubjectId(), "ACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        Coach coach = null;
        if (requestDTO.getCoachId() != null) {
            coach = coachRepository.findById(requestDTO.getCoachId())
                    .orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
            if (coach.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.COACH_INACTIVE);
            }
            CoachSubjectGuard.requireTeaches(coach, subject);
        }

        sportClass.setRoom(room);
        sportClass.setSubject(subject);
        sportClass.setCoach(coach);
        sportClass.setMaxCapacity(requestDTO.getMaxCapacity());
        if (image != null && !image.isEmpty()) {
            sportClass.setImageUrl(imageStorageService.uploadClassImage(image));
        }

        if (scheduleChanged) {
            // Xoá các buổi cũ và tạo lại toàn bộ
            classSessionRepository.deleteBySportClass_ClassId(id);

            if (requestDTO.getStartDate() != null && requestDTO.getDurationDays() != null && requestDTO.getDurationDays() > 0) {
                if (requestDTO.getStartDate().isBefore(LocalDate.now())) {
                    throw new AppException(ErrorCode.CLASS_START_DATE_INVALID);
                }
                if (requestDTO.getStartClock() == null || requestDTO.getEndClock() == null
                        || !requestDTO.getEndClock().isAfter(requestDTO.getStartClock())) {
                    throw new AppException(ErrorCode.INVALID_CLASS_TIME);
                }

                Set<DayOfWeek> days = parseDaysOfWeek(requestDTO.getDaysOfWeek());
                LocalDate endDate = requestDTO.getStartDate().plusDays(requestDTO.getDurationDays());

                List<ClassSession> sessionsToCreate = new ArrayList<>();
                for (LocalDate d = requestDTO.getStartDate(); !d.isAfter(endDate); d = d.plusDays(1)) {
                    if (days.contains(d.getDayOfWeek())) {
                        LocalDateTime sStart = d.atTime(requestDTO.getStartClock());
                        LocalDateTime sEnd = d.atTime(requestDTO.getEndClock());

                        if (classSessionRepository.existsRoomConflict(room.getRoomId(), sStart, sEnd, null)
                                || sportClassRepository.existsRoomConflict(room.getRoomId(), sStart, sEnd, id)) {
                            throw new AppException(ErrorCode.SESSION_SCHEDULE_CONFLICT);
                        }
                        if (coach != null && (classSessionRepository.existsCoachConflict(coach.getUserId(), sStart, sEnd, null)
                                || sportClassRepository.existsCoachConflict(coach.getUserId(), sStart, sEnd, id))) {
                            throw new AppException(ErrorCode.SESSION_SCHEDULE_CONFLICT);
                        }

                        sessionsToCreate.add(ClassSession.builder()
                                .sportClass(sportClass)
                                .startTime(sStart)
                                .endTime(sEnd)
                                .coach(coach)
                                .room(room)
                                .type(SessionType.REGULAR)
                                .status(SessionStatus.SCHEDULED)
                                .build());
                    }
                }

                if (sessionsToCreate.isEmpty()) {
                    throw new AppException(ErrorCode.CLASS_DAYS_OF_WEEK_INVALID);
                }

                LocalDateTime firstSessionAt = sessionsToCreate.get(0).getStartTime();
                LocalDateTime lastSessionEnd = sessionsToCreate.get(sessionsToCreate.size() - 1).getEndTime();

                sportClass.setStartDate(requestDTO.getStartDate());
                sportClass.setEndDate(endDate);
                sportClass.setDurationDays(requestDTO.getDurationDays());
                sportClass.setDaysOfWeek(requestDTO.getDaysOfWeek());
                sportClass.setStartClock(requestDTO.getStartClock());
                sportClass.setEndClock(requestDTO.getEndClock());
                sportClass.setFirstSessionAt(firstSessionAt);
                sportClass.setStartTime(firstSessionAt);
                sportClass.setEndTime(lastSessionEnd);

                classSessionRepository.saveAll(sessionsToCreate);
            } else {
                validateClassTime(requestDTO.getStartTime(), requestDTO.getEndTime());
                checkScheduleConflict(requestDTO.getRoomId(), requestDTO.getCoachId(), requestDTO.getStartTime(), requestDTO.getEndTime(), id);

                sportClass.setStartDate(requestDTO.getStartTime().toLocalDate());
                sportClass.setEndDate(requestDTO.getEndTime().toLocalDate());
                sportClass.setDurationDays(1);
                sportClass.setDaysOfWeek(requestDTO.getStartTime().getDayOfWeek().name());
                sportClass.setStartClock(requestDTO.getStartTime().toLocalTime());
                sportClass.setEndClock(requestDTO.getEndTime().toLocalTime());
                sportClass.setFirstSessionAt(requestDTO.getStartTime());
                sportClass.setStartTime(requestDTO.getStartTime());
                sportClass.setEndTime(requestDTO.getEndTime());

                ClassSession session = ClassSession.builder()
                        .sportClass(sportClass)
                        .startTime(requestDTO.getStartTime())
                        .endTime(requestDTO.getEndTime())
                        .coach(coach)
                        .room(room)
                        .type(SessionType.REGULAR)
                        .status(SessionStatus.SCHEDULED)
                        .build();
                classSessionRepository.save(session);
            }
        }

        SportClass saved = sportClassRepository.save(sportClass);
        return toResponseDTO(saved);
    }

    private boolean isScheduleChanged(SportClass c, SportClassRequestDTO r) {
        if (r.getStartDate() != null && !Objects.equals(c.getStartDate(), r.getStartDate())) return true;
        if (r.getDurationDays() != null && !Objects.equals(c.getDurationDays(), r.getDurationDays())) return true;
        if (r.getDaysOfWeek() != null && !Objects.equals(c.getDaysOfWeek(), r.getDaysOfWeek())) return true;
        if (r.getStartClock() != null && !Objects.equals(c.getStartClock(), r.getStartClock())) return true;
        if (r.getEndClock() != null && !Objects.equals(c.getEndClock(), r.getEndClock())) return true;
        if (r.getStartTime() != null && !Objects.equals(c.getStartTime(), r.getStartTime())) return true;
        if (r.getEndTime() != null && !Objects.equals(c.getEndTime(), r.getEndTime())) return true;
        return false;
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
                Sort.by(Sort.Direction.ASC, "startTime"));
        Page<SportClass> result = sportClassRepository.search(from, to, subjectId, roomId, coachId,
                statusFilter, available, pageable);

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

        List<ClassRegistration> active = classRegistrationRepository.findActiveByClassId(id);
        List<Member> members = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ClassRegistration r : active) {
            r.setStatus("CANCELLED");
            r.setCancelledAt(now);
            subscriptionService.refundSession(r);
            members.add(r.getMember());
        }
        classRegistrationRepository.saveAll(active);

        // Chuyển toàn bộ buổi học chưa hoàn thành sang CANCELLED
        List<ClassSession> sessions = classSessionRepository.findBySportClass_ClassIdOrderByStartTimeAsc(id);
        for (ClassSession s : sessions) {
            if (s.getStatus() == SessionStatus.SCHEDULED) {
                s.setStatus(SessionStatus.CANCELLED);
                s.setNote((s.getNote() != null ? s.getNote() + " | " : "") + "Lớp bị huỷ: " + (reason != null ? reason : ""));
            }
        }
        classSessionRepository.saveAll(sessions);

        sportClass.setStatus(ClassStatus.CANCELLED.name());
        sportClassRepository.save(sportClass);

        String content = "Lớp " + sportClass.getSubject().getSubjectName() + " đã bị hủy.";
        if (reason != null && !reason.isBlank()) {
            content += " Lý do: " + reason.trim();
        }
        notificationService.notifyUsers(members, "Lớp học bị hủy", content, "CLASS_CANCELLED");

        return toResponse(sportClass, 0L);
    }

    @Override
    @Transactional
    public SportClassResponseDTO stopClass(Long id, String reason) {
        SportClass sportClass = sportClassRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (ClassStatus.CANCELLED.name().equals(sportClass.getStatus()) || "STOPPED".equals(sportClass.getStatus())) {
            throw new AppException(ErrorCode.CLASS_ALREADY_STOPPED);
        }

        // Chuyển các buổi học trong tương lai sang CANCELLED
        List<ClassSession> futureSessions = classSessionRepository.findBySportClass_ClassIdAndStartTimeAfterOrderByStartTimeAsc(id, LocalDateTime.now());
        for (ClassSession s : futureSessions) {
            if (s.getStatus() == SessionStatus.SCHEDULED) {
                s.setStatus(SessionStatus.CANCELLED);
                s.setNote((s.getNote() != null ? s.getNote() + " | " : "") + "Dừng khẩn cấp: " + (reason != null ? reason : ""));
            }
        }
        classSessionRepository.saveAll(futureSessions);

        sportClass.setStatus("STOPPED");
        SportClass saved = sportClassRepository.save(sportClass);

        List<Member> members = registeredMembers(id);
        if (!members.isEmpty()) {
            String content = "Lớp " + sportClass.getSubject().getSubjectName() + " đã dừng hoạt động khẩn cấp.";
            if (reason != null && !reason.isBlank()) {
                content += " Lý do: " + reason.trim();
            }
            notificationService.notifyUsers(members, "Dừng lớp khẩn cấp", content, "CLASS_STOPPED");
        }

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public SportClassResponseDTO completeClass(Long id) {
        SportClass sportClass = getClassById(id);
        requireOpening(sportClass);

        if (sportClass.getEndTime() == null || sportClass.getEndTime().isAfter(LocalDateTime.now())) {
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

    private Set<DayOfWeek> parseDaysOfWeek(String daysStr) {
        if (daysStr == null || daysStr.isBlank()) {
            throw new AppException(ErrorCode.CLASS_DAYS_OF_WEEK_INVALID);
        }
        Set<DayOfWeek> result = new LinkedHashSet<>();
        String[] tokens = daysStr.split("[,;\\s]+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            String t = token.trim().toUpperCase();
            switch (t) {
                case "MON": case "MONDAY": case "2": case "T2": case "THU2": case "THỨ2":
                    result.add(DayOfWeek.MONDAY); break;
                case "TUE": case "TUESDAY": case "3": case "T3": case "THU3": case "THỨ3":
                    result.add(DayOfWeek.TUESDAY); break;
                case "WED": case "WEDNESDAY": case "4": case "T4": case "THU4": case "THỨ4":
                    result.add(DayOfWeek.WEDNESDAY); break;
                case "THU": case "THURSDAY": case "5": case "T5": case "THU5": case "THỨ5":
                    result.add(DayOfWeek.THURSDAY); break;
                case "FRI": case "FRIDAY": case "6": case "T6": case "THU6": case "THỨ6":
                    result.add(DayOfWeek.FRIDAY); break;
                case "SAT": case "SATURDAY": case "7": case "T7": case "THU7": case "THỨ7":
                    result.add(DayOfWeek.SATURDAY); break;
                case "SUN": case "SUNDAY": case "8": case "CN": case "CHUNHAT": case "CHỦNHẬT":
                    result.add(DayOfWeek.SUNDAY); break;
                default:
                    try {
                        result.add(DayOfWeek.valueOf(t));
                    } catch (IllegalArgumentException e) {
                        throw new AppException(ErrorCode.CLASS_DAYS_OF_WEEK_INVALID);
                    }
            }
        }
        if (result.isEmpty()) {
            throw new AppException(ErrorCode.CLASS_DAYS_OF_WEEK_INVALID);
        }
        return result;
    }

    private String formatScheduleSummary(SportClass c) {
        if (c.getDaysOfWeek() != null && c.getStartClock() != null && c.getEndClock() != null) {
            DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String daysStr = c.getDaysOfWeek();
            try {
                Set<DayOfWeek> days = parseDaysOfWeek(c.getDaysOfWeek());
                List<String> vnDays = new ArrayList<>();
                for (DayOfWeek d : days) {
                    switch (d) {
                        case MONDAY -> vnDays.add("Thứ 2");
                        case TUESDAY -> vnDays.add("Thứ 3");
                        case WEDNESDAY -> vnDays.add("Thứ 4");
                        case THURSDAY -> vnDays.add("Thứ 5");
                        case FRIDAY -> vnDays.add("Thứ 6");
                        case SATURDAY -> vnDays.add("Thứ 7");
                        case SUNDAY -> vnDays.add("Chủ nhật");
                    }
                }
                daysStr = String.join(", ", vnDays);
            } catch (Exception ignored) {}

            String timeRange = c.getStartClock().format(clockFmt) + " - " + c.getEndClock().format(clockFmt);
            String dateRange = "";
            if (c.getStartDate() != null && c.getEndDate() != null) {
                dateRange = " (Từ " + c.getStartDate().format(dateFmt) + " đến " + c.getEndDate().format(dateFmt) + ")";
            }
            return daysStr + " (" + timeRange + ")" + dateRange;
        } else if (c.getStartTime() != null && c.getEndTime() != null) {
            return c.getStartTime().format(TIME_FMT) + " - " + c.getEndTime().format(TIME_FMT);
        }
        return "Chưa có lịch cố định";
    }

    private SportClassResponseDTO toResponse(SportClass c, long registeredCount) {
        long max = c.getMaxCapacity() != null ? c.getMaxCapacity() : 0;
        int totalSessions = (int) classSessionRepository.countBySportClass_ClassId(c.getClassId());
        return SportClassResponseDTO.builder()
                .classId(c.getClassId())
                .maxCapacity(c.getMaxCapacity())
                .startTime(c.getStartTime())
                .endTime(c.getEndTime())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .durationDays(c.getDurationDays())
                .daysOfWeek(c.getDaysOfWeek())
                .startClock(c.getStartClock())
                .endClock(c.getEndClock())
                .firstSessionAt(c.getFirstSessionAt())
                .totalSessions(totalSessions)
                .scheduleSummary(formatScheduleSummary(c))
                .status(c.getStatus())
                .imageUrl(c.getImageUrl())
                .roomId(c.getRoom() != null ? c.getRoom().getRoomId() : null)
                .roomName(c.getRoom() != null ? c.getRoom().getRoomName() : null)
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .coachId(c.getCoach() != null ? c.getCoach().getUserId() : null)
                .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
                .currentEnrolled(registeredCount)
                .availableSeats(Math.max(0L, max - registeredCount))
                .build();
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        SportClass sportClass = getClassById(id);
        if (!sportClass.getStartTime().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CLASS_ALREADY_STARTED);
        }

        LocalDateTime now = LocalDateTime.now();
        for (ClassRegistration r : classRegistrationRepository.findActiveByClassId(id)) {
            r.setStatus("CANCELLED");
            r.setCancelledAt(now);
            subscriptionService.refundSession(r);
        }

        List<ClassSession> sessions = classSessionRepository.findBySportClass_ClassIdOrderByStartTimeAsc(id);
        for (ClassSession s : sessions) {
            s.setStatus(SessionStatus.CANCELLED);
        }
        classSessionRepository.saveAll(sessions);

        sportClass.setStatus("INACTIVE");
        sportClassRepository.save(sportClass);
    }

    private void validateClassTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new AppException(ErrorCode.INVALID_CLASS_TIME);
        }
    }

    private void checkScheduleConflict(Long roomId, Long coachId, LocalDateTime startTime, LocalDateTime endTime, Long excludeClassId) {
        if (sportClassRepository.existsRoomConflict(roomId, startTime, endTime, excludeClassId)
                || classSessionRepository.existsRoomConflict(roomId, startTime, endTime, null)) {
            throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
        }
        if (coachId != null && (sportClassRepository.existsCoachConflict(coachId, startTime, endTime, excludeClassId)
                || classSessionRepository.existsCoachConflict(coachId, startTime, endTime, null))) {
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
        return toResponse(c, enrolled);
    }
}
