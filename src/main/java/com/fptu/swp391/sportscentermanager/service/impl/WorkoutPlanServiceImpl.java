package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.WorkoutPlanService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class WorkoutPlanServiceImpl implements WorkoutPlanService {
    private final CoachRepository coachRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final MemberPlanRepository memberPlanRepository;
    private final MemberRepository memberRepository;
    private final SportClassRepository sportClassRepository;
    private final ClassPlanRepository classPlanRepository;

    public WorkoutPlanServiceImpl(CoachRepository coachRepository, WorkoutPlanRepository workoutPlanRepository, MemberPlanRepository memberPlanRepository, MemberRepository memberRepository, SportClassRepository sportClassRepository, ClassPlanRepository classPlanRepository) {
        this.coachRepository = coachRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.memberPlanRepository = memberPlanRepository;
        this.memberRepository = memberRepository;
        this.sportClassRepository = sportClassRepository;
        this.classPlanRepository = classPlanRepository;
    }

    @Override
    public WorkoutPlanResponseDTO createAndAssignWorkoutPlan(Long coachId, WorkoutPlanRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));

        WorkoutPlan plan = WorkoutPlan.builder()
            .planName(requestDTO.getPlanName())
            .exerciseDetails(requestDTO.getExerciseDetails())
            .duration(requestDTO.getDuration())
            .coach(coach)
            .createdAt(LocalDateTime.now())
            .build();
    WorkoutPlan savedPlan = workoutPlanRepository.save(plan);

    String targetName = "";
    LocalDate now = LocalDate.now();

    if ("INDIVIDUAL".equalsIgnoreCase(requestDTO.getTargetType())){
        Member member =memberRepository.findById(requestDTO.getTargetId()).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        MemberPlan memberPlan = MemberPlan.builder().workoutPlan(savedPlan).member(member)
            .assignedDate(now)
            .dueDate((requestDTO.getDueDate() != null ? requestDTO.getDueDate() : now.plusWeeks(4)).atStartOfDay()).status("IN_PROGRESS")
            .build();
        memberPlanRepository.save(memberPlan);

        targetName = member.getFirstName() + " " + member.getLastName();
    } else if("CLASS".equalsIgnoreCase(requestDTO.getTargetType())) {
        SportClass sportClass = sportClassRepository.findById(requestDTO.getTargetId()).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (sportClass.getCoach() == null || !sportClass.getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        ClassPlan classPlan = ClassPlan.builder()
            .workoutPlan(savedPlan)
            .sportClass(sportClass)
            .assignedDate(now.atStartOfDay())
            .build();
        classPlanRepository.save(classPlan);
        targetName = sportClass.getSubject() != null ? sportClass.getSubject().getSubjectName() : "Lớp " + sportClass.getClassId();

    } else {
        throw new IllegalArgumentException("Loại đối tượng không hợp lệ! Chỉ chấp nhận INDIVIDUAL hoặc CLASS");
    }
    return WorkoutPlanResponseDTO.builder()
        .planId(savedPlan.getPlanId())
        .planName(savedPlan.getPlanName())
        .exerciseDetails(savedPlan.getExerciseDetails())
        .duration(savedPlan.getDuration())
        .coachId(coach.getUserId())
        .coachName(coach.getFirstName() + " " + coach.getLastName())
        .targetId(requestDTO.getTargetId())
        .targetType(requestDTO.getTargetType())
        .targetName(targetName)
        .assignedDate(now)
        .dueDate(requestDTO.getDueDate())
        .status("IN_PROGRESS")
        .createdAt(savedPlan.getCreatedAt())
        .build();
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByCoach(Long coachId) {
        List<WorkoutPlan> plans = workoutPlanRepository.findByCoach_UserIdOrderByCreatedAtDesc(coachId);
        List<WorkoutPlanResponseDTO> dtos = new ArrayList<>();
        for (WorkoutPlan p : plans) {
            Coach c = p.getCoach();
            dtos.add(WorkoutPlanResponseDTO.builder()
                    .planId(p.getPlanId())
                    .planName(p.getPlanName())
                    .exerciseDetails(p.getExerciseDetails())
                    .duration(p.getDuration())
                    .coachId(coachId)
                    .coachName(c != null ? c.getFirstName() + " " + c.getLastName() : "")
                    .createdAt(p.getCreatedAt())
                .build());
        }
        return dtos;
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByMember(Long memberId) {
        List<MemberPlan> memberPlans = memberPlanRepository.findByMember_UserId(memberId);
        List<WorkoutPlanResponseDTO> result = new ArrayList<>();
        for (MemberPlan mp : memberPlans) {
            WorkoutPlan wp = mp.getWorkoutPlan();
            Member m = mp.getMember();
            Coach c = wp.getCoach();
            result.add(WorkoutPlanResponseDTO.builder()
                    .planId(wp.getPlanId())
                    .planName(wp.getPlanName())
                    .exerciseDetails(wp.getExerciseDetails())
                    .duration(wp.getDuration())
                    .coachId(c != null ? c.getUserId() : null)
                    .coachName(c != null ? c.getFirstName() + " " + c.getLastName() : "")
                    .memberPlanId(mp.getMemberPlanId())
                    .targetType("INDIVIDUAL")
                    .targetId(memberId)
                    .targetName(m != null ? m.getFirstName() + " " + m.getLastName() : "")
                    .assignedDate(mp.getAssignedDate())
                    .dueDate(mp.getDueDate() != null ? mp.getDueDate().toLocalDate() : null)
                    .status(mp.getStatus())
                    .createdAt(wp.getCreatedAt())
                    .build());
        }
        return result;
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByClass(Long classId) {
        List<ClassPlan> classPlans = classPlanRepository.findBySportClass_ClassId(classId);
        List<WorkoutPlanResponseDTO> result = new ArrayList<>();
        for (ClassPlan cp : classPlans) {
            WorkoutPlan wp = cp.getWorkoutPlan();
            SportClass sc = cp.getSportClass();
            Coach c = wp.getCoach();
            String className = (sc != null && sc.getSubject() != null)
                    ? sc.getSubject().getSubjectName()
                    : (sc != null ? "Lớp " + sc.getClassId() : "");
            result.add(WorkoutPlanResponseDTO.builder()
                    .planId(wp.getPlanId())
                    .planName(wp.getPlanName())
                    .exerciseDetails(wp.getExerciseDetails())
                    .duration(wp.getDuration())
                    .coachId(c != null ? c.getUserId() : null)
                    .coachName(c != null ? c.getFirstName() + " " + c.getLastName() : "")
                    .targetType("CLASS")
                    .targetId(classId)
                    .targetName(className)
                    .assignedDate(cp.getAssignedDate() != null ? cp.getAssignedDate().toLocalDate() : null)
                    .status("ACTIVE")
                    .createdAt(wp.getCreatedAt())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public void updateMemberPlanStatus(Long coachId, Long memberPlanId, String status) {
        MemberPlan memberPlan = memberPlanRepository.findById(memberPlanId).orElseThrow(() -> new AppException(ErrorCode.RESOUCE_NOT_FOUND));
        // kiem tra giao an nay co dung do HLV nay giao khong
        if (!memberPlan.getWorkoutPlan().getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        memberPlan.setStatus(status.toUpperCase());
        memberPlanRepository.save(memberPlan);
    }
}
