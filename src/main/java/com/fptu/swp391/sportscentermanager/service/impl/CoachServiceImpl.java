package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.CoachService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public CoachResponseDTO createCoach(CoachRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }

        Role coachRole = roleRepository.findByRoleName("COACH");

        Coach newCoach = Coach.builder()
            .firstName(requestDTO.getFirstName())
            .lastName(requestDTO.getLastName())
            .gender(requestDTO.getGender())
            .phone(requestDTO.getPhone())
            .email(requestDTO.getEmail())
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .status("ACTIVE")
            .role(coachRole)
            .speciality(requestDTO.getSpecialty())
            .build();

        Coach savedCoach = coachRepository.save(newCoach);
        return CoachResponseDTO.builder()
            .coachId(savedCoach.getUserId())
            .firstName(savedCoach.getFirstName())
            .lastName(savedCoach.getLastName())
            .email(savedCoach.getEmail())
            .phone(savedCoach.getPhone())
            .speciality(savedCoach.getSpeciality())
            .build();
    }

    @Override
    public List<CoachResponseDTO> getAllCoaches() {
        List<Coach> coachList = coachRepository.findAll();
        List<CoachResponseDTO> result = new ArrayList<>();

        for (Coach c : coachList) {
            CoachResponseDTO dto = CoachResponseDTO.builder()
                .coachId(c.getUserId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .speciality(c.getSpeciality())
                .build();
            result.add(dto);
        }
        return result;
    }

    @Override
    public CoachResponseDTO getCoachById(Long id) {
        Coach c = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        if ("INACTIVE".equals(c.getStatus())) {
            throw new AppException(ErrorCode.COACH_NOT_FOUND);
        }
        return CoachResponseDTO.builder()
            .coachId(c.getUserId())
            .firstName(c.getFirstName())
            .lastName(c.getLastName())
            .phone(c.getPhone())
            .email(c.getEmail())
            .speciality(c.getSpeciality())
            .build();
    }

    @Override
    public CoachResponseDTO updateCoach(Long id, CoachRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        if (!coach.getEmail().equals(requestDTO.getEmail()) && userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }

        coach.setFirstName(requestDTO.getFirstName());
        coach.setLastName(requestDTO.getLastName());
        coach.setGender(requestDTO.getGender());
        coach.setPhone(requestDTO.getPhone());
        coach.setEmail(requestDTO.getEmail());
        coach.setSpeciality(requestDTO.getSpecialty());

        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isBlank()){
            coach.setPasswordHash(passwordEncoder.encode(requestDTO.getPassword()));
        }

        Coach savedCoach = coachRepository.save(coach);

        return CoachResponseDTO.builder()
            .coachId(savedCoach.getUserId())
            .firstName(savedCoach.getFirstName())
            .lastName(savedCoach.getLastName())
            .email(savedCoach.getEmail())
            .phone(savedCoach.getPhone())
            .speciality(savedCoach.getSpeciality())
            .build();
    }

    @Override
    public void deleteCoach(Long id) {
        Coach coach = coachRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        coach.setStatus("INACTIVE");
        coachRepository.save(coach);
    }

    @Override
    public List<TeachingScheduleResponseDTO> getMyTeachingSchedule(Long coachId) {
        List<SportClass> classes = sportClassRepository.findByCoach_UserIdOrderByScheduleTimeAsc(coachId);
        List<TeachingScheduleResponseDTO> result = new ArrayList<>();

        for (SportClass c : classes) {
            long enrolledCnt = classEnrollmentRepository.countBySportClass_ClassIdAndStatus(c.getClassId(), "ENROLLED");
            String subjectName = (c.getSubject() != null) ? c.getSubject().getSubjectName() : "Chua co mon nao";
            String roomName = (c.getRoom() != null) ? c.getRoom().getRoomName() : "Chua co phong";

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
    public List<ClassRosterResponseDTO> getClassRoster(Long coachId, Long classId) {
        SportClass sportClass = sportClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

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
    public List<AttendanceResponseDTO> takeAttendance(Long coachId, Long classId, TakeAttendanceRequestDTO requestDTO) {
        // check lop ton tai va co phai coach do phu trach khong
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        LocalDateTime attendanceDate = sportClass.getScheduleTime();

        // lay danh sach id member da dang ki trong lop
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findBySportClass_ClassId(classId);
        Set<Long> enrollmentIds = new HashSet<>();
        for (ClassEnrollment e : enrollments) {
            if (e.getMember() != null){
                enrollmentIds.add(e.getMember().getUserId());
            }
        }

        List<ClassAttendance> savedAttendance = new ArrayList<>();

        for(AttendanceRecordDTO record : requestDTO.getRecords()){
            Long memberId = record.getMemberId();
            if (!enrollmentIds.contains(memberId)){
                throw new AppException(ErrorCode.MEMBER_NOT_IN_CLASS);
            }

            Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

            Optional<ClassAttendance> existngOpt = classAttendanceRepository.findBySportClass_ClassIdAndMember_UserId(classId, memberId);

            ClassAttendance attendance;
            if (existngOpt.isPresent()) {
                attendance = existngOpt.get();
            } else {
                attendance = ClassAttendance.builder()
                    .sportClass(sportClass)
                    .member(member)
                    .attendanceDate(attendanceDate)
                    .build();
            }
            attendance.setStatus(record.getStatus());            attendance.setNote(record.getNote());
            attendance.setMarkedAt(LocalDateTime.now());

            ClassAttendance saved = classAttendanceRepository.save(attendance);
            savedAttendance.add(saved);
        }

        List<AttendanceResponseDTO> result = new ArrayList<>();
        for(ClassAttendance a :  savedAttendance){
            Member m =  a.getMember();
            AttendanceResponseDTO dto = AttendanceResponseDTO.builder()
                .attendanceId(a.getAttendanceId())
                .memberId(m.getUserId())
                .memberName(m.getFirstName() + " " + m.getLastName())
                .phone(m.getPhone())
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .note(a.getNote())
                .markedAt(a.getMarkedAt())
                .build();
            result.add(dto);
        }
        return result;

    }

    @Override
    public List<AttendanceResponseDTO> getClassAttendance(Long coachId, Long classId) {
        // kiem tra lop
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        // lay danh sach diem danh cua lop
        List<ClassAttendance> attendances = classAttendanceRepository.findBySportClass_ClassId(classId);

        // map sang responsedto
        List<AttendanceResponseDTO> result = new ArrayList<>();
        for(ClassAttendance a : attendances){
            Member m =  a.getMember();
            AttendanceResponseDTO dto = AttendanceResponseDTO.builder()
                .attendanceId(a.getAttendanceId())
                .memberId(m.getUserId())
                .memberName(m.getFirstName() + " " + m.getLastName())
                .phone(m.getPhone())
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .note(a.getNote())
                .markedAt(a.getMarkedAt())
                .build();
            result.add(dto);
        }
        return result;
    }

}

