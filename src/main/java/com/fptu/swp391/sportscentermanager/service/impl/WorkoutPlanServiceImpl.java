package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.NotificationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutPlanResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
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
    private final NotificationService notificationService;

    public WorkoutPlanServiceImpl(CoachRepository coachRepository, WorkoutPlanRepository workoutPlanRepository, MemberPlanRepository memberPlanRepository, MemberRepository memberRepository, SportClassRepository sportClassRepository, ClassPlanRepository classPlanRepository, NotificationService notificationService) {
        this.coachRepository = coachRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.memberPlanRepository = memberPlanRepository;
        this.memberRepository = memberRepository;
        this.sportClassRepository = sportClassRepository;
        this.classPlanRepository = classPlanRepository;
        this.notificationService = notificationService;
    }

    @Override
    public WorkoutPlanResponseDTO createAndAssignWorkoutPlan(Long coachId, WorkoutPlanRequestDTO requestDTO) {
        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));
        // không cho giao giáo án có hạn trong quá khứ (chặn trước khi lưu gì)
        if (requestDTO.getDueDate() != null && requestDTO.getDueDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.WORKOUT_PLAN_DUE_DATE_PAST);
        }

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
        notificationService.sendNotificationToMember(coachId, member.getUserId(), planNotice(savedPlan));
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
        targetName = sportClass.displayName();
        // báo cho học viên đang học lớp; đẩy realtime sau khi commit
        notificationService.sendNotificationToClass(coachId, sportClass.getClassId(), planNotice(savedPlan));

    } else {
        throw new AppException(ErrorCode.WORKOUT_PLAN_INVALID_TARGET);
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
        List<WorkoutPlan> plans =
            workoutPlanRepository
                .findByCoach_UserIdOrderByCreatedAtDesc(coachId);

        List<MemberPlan> personalAssignments =
            memberPlanRepository
                .findByWorkoutPlan_Coach_UserId(coachId);

        List<ClassPlan> classAssignments =
            classPlanRepository
                .findByWorkoutPlan_Coach_UserId(coachId);

        List<WorkoutPlanResponseDTO> result = new ArrayList<>();

        for (WorkoutPlan plan : plans) {
            Coach coach = plan.getCoach();

            WorkoutPlanResponseDTO dto =
                WorkoutPlanResponseDTO.builder()
                    .planId(plan.getPlanId())
                    .planName(plan.getPlanName())
                    .exerciseDetails(plan.getExerciseDetails())
                    .duration(plan.getDuration())
                    .coachId(coachId)
                    .coachName(
                        coach == null
                            ? ""
                            : coach.getFirstName() + " " + coach.getLastName()
                    )
                    .createdAt(plan.getCreatedAt())
                    .build();

            MemberPlan personalAssignment =
                personalAssignments.stream()
                    .filter(assignment ->
                        assignment.getWorkoutPlan().getPlanId()
                            .equals(plan.getPlanId())
                    )
                    .findFirst()
                    .orElse(null);

            ClassPlan classAssignment =
                classAssignments.stream()
                    .filter(assignment ->
                        assignment.getWorkoutPlan().getPlanId()
                            .equals(plan.getPlanId())
                    )
                    .findFirst()
                    .orElse(null);

            if (personalAssignment != null) {
                Member member = personalAssignment.getMember();

                dto.setTargetType("INDIVIDUAL");
                dto.setTargetId(member.getUserId());
                dto.setTargetName(
                    member.getFirstName() + " " + member.getLastName()
                );
                dto.setMemberPlanId(
                    personalAssignment.getMemberPlanId()
                );
                dto.setAssignedDate(
                    personalAssignment.getAssignedDate()
                );
                dto.setDueDate(
                    personalAssignment.getDueDate() == null
                        ? null
                        : personalAssignment.getDueDate().toLocalDate()
                );
                dto.setStatus(personalAssignment.getStatus());

            } else if (classAssignment != null) {
                SportClass sportClass =
                    classAssignment.getSportClass();


                dto.setTargetType("CLASS");
                dto.setTargetId(sportClass.getClassId());
                dto.setTargetName(sportClass.displayName());
                dto.setAssignedDate(
                    classAssignment.getAssignedDate() == null
                        ? null
                        : classAssignment.getAssignedDate().toLocalDate()
                );
                dto.setStatus(classPlanStatus(sportClass));
                dto.setDueDate(sportClass.getEndDate());
            }

            result.add(dto);
        }

        return result;
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

        for (ClassPlan classPlan :
            classPlanRepository.findVisibleToMember(memberId)) {

            WorkoutPlan plan = classPlan.getWorkoutPlan();
            Coach coach = plan.getCoach();
            SportClass sportClass = classPlan.getSportClass();


            result.add(
                WorkoutPlanResponseDTO.builder()
                    .planId(plan.getPlanId())
                    .planName(plan.getPlanName())
                    .exerciseDetails(plan.getExerciseDetails())
                    .duration(plan.getDuration())
                    .coachId(
                        coach == null ? null : coach.getUserId()
                    )
                    .coachName(
                        coach == null
                            ? ""
                            : coach.getFirstName() + " " + coach.getLastName()
                    )
                    .targetType("CLASS")
                    .targetId(sportClass.getClassId())
                    .targetName(sportClass.displayName())
                    .dueDate(sportClass.getEndDate())
                    .assignedDate(
                        classPlan.getAssignedDate() == null
                            ? null
                            : classPlan.getAssignedDate().toLocalDate()
                    )
                    .status(classPlanStatus(sportClass))
                    .createdAt(plan.getCreatedAt())
                    .build()
            );
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
            String className = sc != null ? sc.displayName() : "";
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
                    .dueDate(sc != null ? sc.getEndDate() : null)
                    .status(classPlanStatus(sc))
                    .createdAt(wp.getCreatedAt())
                    .build());
        }
        return result;
    }

    // Giáo án lớp không có nút hoàn thành: lớp đã kết thúc (qua ngày kết thúc) thì coi như hoàn thành
    private static String classPlanStatus(SportClass c) {
        return c != null && c.getEndDate() != null && c.getEndDate().isBefore(LocalDate.now()) ? "COMPLETED" : "ACTIVE";
    }

    @Override
    @Transactional
    public void updateMemberPlanStatus(Long coachId, Long memberPlanId, String status) {
        MemberPlan memberPlan = memberPlanRepository.findById(memberPlanId).orElseThrow(() -> new AppException(ErrorCode.RESOUCE_NOT_FOUND));
        // kiem tra giao an nay co dung do HLV nay giao khong
        if (!memberPlan.getWorkoutPlan().getCoach().getUserId().equals(coachId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // chỉ nhận 2 trạng thái là IN_PROGRESS hoặc COMPLETED
        String newStatus = status == null ? "" : status.trim().toUpperCase();
        if (!newStatus.equals("IN_PROGRESS") && !newStatus.equals("COMPLETED")) {
            throw new AppException(ErrorCode.WORKOUT_PLAN_INVALID_STATUS);
        }
        memberPlan.setStatus(newStatus);
        memberPlanRepository.save(memberPlan);
    }

    private NotificationRequestDTO planNotice(WorkoutPlan plan) {
        return NotificationRequestDTO.builder()
            .title("Giáo án mới: " + plan.getPlanName())
            .content("Huấn luyện viên vừa giao giáo án \"" + plan.getPlanName() + "\""
                + (plan.getDuration() != null && !plan.getDuration().isBlank() ? " (" + plan.getDuration() + ")" : "")
                + ". Xem chi tiết trong mục Tập luyện.")
            .build();
    }
}
