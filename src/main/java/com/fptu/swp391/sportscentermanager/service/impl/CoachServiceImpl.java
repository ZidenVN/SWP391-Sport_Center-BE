package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SportClassRepository sportClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassAttendanceRepository classAttendanceRepository;
    private final MemberRepository memberRepository;

    private SportClass getOwnedClassOrThrow(Long coachId, Long classId) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }
        return sportClass;
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

        Role coachRole = roleRepository.findByRoleName("COACH");
        if (coachRole == null) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        Coach newCoach = Coach.builder()
            .firstName(requestDTO.getFirstName())
            .lastName(requestDTO.getLastName())
            .gender(requestDTO.getGender())
            .phone(requestDTO.getPhone())
            .email(requestDTO.getEmail())
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .status(UserStatus.ACTIVE)
            .role(coachRole)
            .speciality(requestDTO.getSpecialty())
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
        Coach c = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        if (UserStatus.INACTIVE == c.getStatus()) {
            throw new AppException(ErrorCode.COACH_NOT_FOUND);
        }
        return toCoachResponse(c);
    }

    @Override
    @Transactional
    public CoachResponseDTO updateCoach(Long id, CoachRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (!requestDTO.getEmail().equalsIgnoreCase(coach.getEmail()) && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }

        coach.setFirstName(requestDTO.getFirstName());
        coach.setLastName(requestDTO.getLastName());
        coach.setGender(requestDTO.getGender());
        coach.setPhone(requestDTO.getPhone());
        coach.setEmail(requestDTO.getEmail());
        coach.setSpeciality(requestDTO.getSpecialty());

        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isBlank()) {
            coach.setPasswordHash(passwordEncoder.encode(requestDTO.getPassword()));
        }

        Coach savedCoach = coachRepository.save(coach);

       return toCoachResponse(savedCoach);
    }

    @Override
    @Transactional
    public void toggleCoachStatus(Long id) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (UserStatus.ACTIVE == coach.getStatus()) {
            coach.setStatus(UserStatus.INACTIVE);
        } else {
            coach.setStatus(UserStatus.ACTIVE);
        }
        coachRepository.save(coach);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingScheduleResponseDTO> getMyTeachingSchedule(Long coachId) {
        List<SportClass> classes = sportClassRepository.findByCoach_UserIdOrderByScheduleTimeAsc(coachId);
        List<TeachingScheduleResponseDTO> result = new ArrayList<>();

        for (SportClass c : classes) {
            long enrolledCnt = classEnrollmentRepository.countBySportClass_ClassIdAndStatus(c.getClassId(), "ENROLLED");
            String subjectName = (c.getSubject() != null) ? c.getSubject().getSubjectName() : "Chưa có môn nào";
            String roomName = (c.getRoom() != null) ? c.getRoom().getRoomName() : "Chưa có phòng";

            TeachingScheduleResponseDTO dto = TeachingScheduleResponseDTO.builder()
                .classId(c.getClassId())
                .subjectName(subjectName)
                .roomName(roomName)
                .scheduleTime(c.getScheduleTime())
                .maxCapacity(c.getMaxCapacity())
                .currentEnrolled(enrolledCnt)
                .status(c.getStatus())
                .build();
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRosterResponseDTO> getClassRoster(Long coachId, Long classId) {
        getOwnedClassOrThrow(coachId, classId);

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findBySportClass_ClassId(classId);
        List<ClassRosterResponseDTO> result = new ArrayList<>();
        for (ClassEnrollment e : enrollments) {
            ClassRosterResponseDTO dto = ClassRosterResponseDTO.builder()
                .memberId(e.getMember().getUserId())
                .firstName(e.getMember().getFirstName())
                .lastName(e.getMember().getLastName())
                .email(e.getMember().getEmail())
                .phone(e.getMember().getPhone())
                .gender(e.getMember().getGender())
                .trainingGoal(e.getMember().getTrainingGoal())
                .enrolledDate(e.getEnrollmentDate())
                .enrollmentStatus(e.getStatus())
                .build();
            result.add(dto);
        }
        return result;

    }

    @Override
    @Transactional
    public List<AttendanceResponseDTO> takeAttendance(Long coachId, Long classId, TakeAttendanceRequestDTO requestDTO) {
        // check lop ton tai va co phai coach do phu trach khong
        SportClass sportClass = getOwnedClassOrThrow(coachId, classId);
        if (requestDTO.getRecords() == null || requestDTO.getRecords().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<Long, Member> enrolledMembers = new HashMap<>();
        for (ClassEnrollment e : classEnrollmentRepository.findBySportClass_ClassId(classId)) {
            if (e.getMember() != null) {
                enrolledMembers.put(e.getMember().getUserId(), e.getMember());
            }
        }


        Map<Long, ClassAttendance> existing = new HashMap<>();
        for (ClassAttendance a : classAttendanceRepository.findBySportClass_ClassId(classId)) {
            existing.put(a.getMember().getUserId(), a);
        }

        Set<Long> seen = new HashSet<>();
        List<ClassAttendance> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (AttendanceRecordDTO record : requestDTO.getRecords()) {
            Long memberId = record.getMemberId();
            Member member = enrolledMembers.get(memberId);
            if (member == null)
                throw new AppException(ErrorCode.MEMBER_NOT_IN_CLASS);
            if (!seen.add(memberId))
                throw new AppException(ErrorCode.DUPLICATE_ATTENDANCE);

            ClassAttendance attendance = existing.getOrDefault(memberId, ClassAttendance.builder()
                .sportClass(sportClass)
                .member(member)
                .attendanceDate(sportClass.getScheduleTime())
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
    public List<AttendanceResponseDTO> getClassAttendance(Long coachId, Long classId) {
       getOwnedClassOrThrow(coachId, classId);
       return classAttendanceRepository.findBySportClass_ClassId(classId).stream().map(this::toAttendanceResponse).toList();
    }

}

