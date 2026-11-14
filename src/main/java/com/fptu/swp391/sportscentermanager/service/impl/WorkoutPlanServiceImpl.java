package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.*;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkoutPlanServiceImpl implements WorkoutPlanService {
    private final CoachRepository coachRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final MemberPlanRepository memberPlanRepository;
    private final MemberRepository memberRepository;
    private final SportClassRepository sportClassRepository;
    private final ClassPlanRepository classPlanRepository;
    private final ClassRegistrationRepository registrations;
    private final NotificationService notifications;

    @Override
    public WorkoutPlanResponseDTO createAndAssignWorkoutPlan(
        Long coachId, WorkoutPlanRequestDTO input) {

        LocalDate today = LocalDate.now(PlanWorkflowService.ZONE);

        if (input.getDueDate() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hạn hoàn thành là bắt buộc"
            );
        }

        if (input.getDueDate().isBefore(today)) {
            throw new AppException(
                ErrorCode.WORKOUT_PLAN_DUE_DATE_PAST
            );
        }

        Coach coach = coachRepository.findById(coachId)
            .orElseThrow(() ->
                new AppException(ErrorCode.COACH_NOT_FOUND)
            );

        WorkoutPlan plan = workoutPlanRepository.save(
            WorkoutPlan.builder()
                .planName(input.getPlanName())
                .exerciseDetails(input.getExerciseDetails())
                .duration(null)
                .coach(coach)
                .createdAt(
                    LocalDateTime.now(PlanWorkflowService.ZONE)
                )
                .build()
        );

        if ("INDIVIDUAL".equalsIgnoreCase(input.getTargetType())) {
            Member member = memberRepository
                .findById(input.getTargetId())
                .orElseThrow(() ->
                    new AppException(ErrorCode.MEMBER_NOT_FOUND)
                );

            MemberPlan row = memberPlanRepository.save(
                MemberPlan.builder()
                    .workoutPlan(plan)
                    .member(member)
                    .assignedDate(today)
                    .dueDate(input.getDueDate().atTime(23, 59, 59))
                    .status("IN_PROGRESS")
                    .build()
            );

            notifications.sendNotificationToMember(
                coachId,
                member.getUserId(),
                NotificationRequestDTO.builder()
                    .title("Giáo án mới: " + plan.getPlanName())
                    .content(
                        "Hạn hết ngày " + input.getDueDate()
                            + " (giờ Việt Nam). Xem mục Tập luyện."
                    )
                    .build()
            );

            return memberDTO(row);
        }

        if (!"CLASS".equalsIgnoreCase(input.getTargetType())) {
            throw new AppException(
                ErrorCode.WORKOUT_PLAN_INVALID_TARGET
            );
        }

        SportClass sc = sportClassRepository
            .findById(input.getTargetId())
            .orElseThrow(() ->
                new AppException(ErrorCode.CLASS_NOT_FOUND)
            );

        if (sc.getCoach() == null
            || !coachId.equals(sc.getCoach().getUserId())) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }

        var recipients = registrations
            .findActiveByClassId(sc.getClassId())
            .stream()
            .map(ClassRegistration::getMember)
            .collect(Collectors.toMap(
                Member::getUserId,
                m -> m,
                (a, b) -> a
            ))
            .values();

        if (recipients.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Lớp chưa có học viên để giao giáo án"
            );
        }

        ClassPlan cp = classPlanRepository.save(
            ClassPlan.builder()
                .workoutPlan(plan)
                .sportClass(sc)
                .assignedDate(
                    LocalDateTime.now(PlanWorkflowService.ZONE)
                )
                .dueDate(input.getDueDate())
                .build()
        );

        memberPlanRepository.saveAll(
            recipients.stream()
                .map(m -> MemberPlan.builder()
                    .workoutPlan(plan)
                    .member(m)
                    .sourceClassId(sc.getClassId())
                    .sourceClassName(subject(sc))
                    .assignedDate(today)
                    .dueDate(input.getDueDate().atTime(23, 59, 59))
                    .status("IN_PROGRESS")
                    .build()
                )
                .toList()
        );

        notifications.notifyClassRecipients(
            coachId,
            sc,
            new ArrayList<>(recipients),
            NotificationRequestDTO.builder()
                .title("Giáo án lớp mới: " + plan.getPlanName())
                .content(
                    "Lớp #" + sc.getClassId()
                        + " · Hạn hết ngày " + input.getDueDate()
                        + " (giờ Việt Nam). Xem mục Tập luyện."
                )
                .build()
        );

        return classDTO(cp);
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByCoach(
        Long coachId) {

        var personal = memberPlanRepository
            .findByWorkoutPlan_Coach_UserId(coachId)
            .stream()
            .filter(p -> p.getSourceClassId() == null)
            .collect(Collectors.toMap(
                p -> p.getWorkoutPlan().getPlanId(),
                p -> p,
                (a, b) -> a
            ));

        var classes = classPlanRepository
            .findByWorkoutPlan_Coach_UserId(coachId)
            .stream()
            .collect(Collectors.toMap(
                p -> p.getWorkoutPlan().getPlanId(),
                p -> p,
                (a, b) -> a
            ));

        return workoutPlanRepository
            .findByCoach_UserIdOrderByCreatedAtDesc(coachId)
            .stream()
            .map(p -> {
                if (classes.containsKey(p.getPlanId())) {
                    return classDTO(classes.get(p.getPlanId()));
                }

                if (personal.containsKey(p.getPlanId())) {
                    return memberDTO(personal.get(p.getPlanId()));
                }

                return base(p);
            })
            .toList();
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByMember(
        Long memberId) {

        List<WorkoutPlanResponseDTO> result = new ArrayList<>(
            memberPlanRepository.findByMember_UserId(memberId)
                .stream()
                .map(this::memberDTO)
                .toList()
        );

        // Giáo án lớp cũ: giữ để đọc.
        // Giáo án mới đã có bản giao riêng cho từng học viên.
        classPlanRepository.findVisibleToMember(memberId)
            .stream()
            .filter(cp -> cp.getDueDate() == null)
            .map(this::classDTO)
            .forEach(result::add);

        return result;
    }

    @Override
    public List<WorkoutPlanResponseDTO> getPlansByClass(
        Long classId) {
        return classPlanRepository
            .findBySportClass_ClassId(classId)
            .stream()
            .map(this::classDTO)
            .toList();
    }

    @Override
    public void updateMemberPlanStatus(
        Long coachId, Long memberPlanId, String status) {

        throw new ResponseStatusException(
            HttpStatus.GONE,
            "Dùng luồng Member báo hoàn thành và Coach duyệt; "
                + "không đổi trạng thái trực tiếp."
        );
    }

    private WorkoutPlanResponseDTO base(WorkoutPlan plan) {
        Coach coach = plan.getCoach();

        return WorkoutPlanResponseDTO.builder()
            .planId(plan.getPlanId())
            .planName(plan.getPlanName())
            .exerciseDetails(plan.getExerciseDetails())
            .duration(plan.getDuration())
            .coachId(coach.getUserId())
            .coachName(name(coach))
            .createdAt(plan.getCreatedAt())
            .build();
    }

    private WorkoutPlanResponseDTO memberDTO(MemberPlan row) {
        var dto = base(row.getWorkoutPlan());
        boolean personal = row.getSourceClassId() == null;

        dto.setMemberPlanId(row.getMemberPlanId());
        dto.setTargetType(personal ? "INDIVIDUAL" : "CLASS");

        dto.setTargetId(
            personal
                ? row.getMember().getUserId()
                : row.getSourceClassId()
        );

        dto.setTargetName(
            personal
                ? name(row.getMember())
                : "Lớp #" + row.getSourceClassId()
                + " · " + row.getSourceClassName()
        );

        dto.setAssignedDate(row.getAssignedDate());
        dto.setDueDate(
            row.getDueDate() == null
                ? null
                : row.getDueDate().toLocalDate()
        );
        dto.setStatus(row.getStatus());

        return dto;
    }

    private WorkoutPlanResponseDTO classDTO(ClassPlan row) {
        var dto = base(row.getWorkoutPlan());

        dto.setTargetType("CLASS");
        dto.setTargetId(row.getSportClass().getClassId());
        dto.setTargetName(
            subject(row.getSportClass())
                + " #" + row.getSportClass().getClassId()
        );
        dto.setAssignedDate(
            row.getAssignedDate() == null
                ? null
                : row.getAssignedDate().toLocalDate()
        );
        dto.setDueDate(row.getDueDate());

        // Trạng thái bản giao chung.
        // Trạng thái từng học viên nằm trong MemberPlan.
        dto.setStatus("ACTIVE");

        return dto;
    }

    private String subject(SportClass sc) {
        return sc.getSubject() == null
            ? "Lớp"
            : sc.getSubject().getSubjectName();
    }

    private String name(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
