package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberProgressResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.MemberPlan;
import com.fptu.swp391.sportscentermanager.entity.WorkoutResult;
import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.ClassAttendanceRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberPlanRepository;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.WorkoutResultRepository;
import com.fptu.swp391.sportscentermanager.service.WorkoutResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutResultServiceImpl implements WorkoutResultService {

    private final WorkoutResultRepository workoutResultRepository;
    private final MemberRepository memberRepository;
    private final MemberPlanRepository memberPlanRepository;
    private final ClassAttendanceRepository classAttendanceRepository;

    @Override
    @Transactional
    public WorkoutResultResponseDTO recordWorkoutResult(WorkoutResultRequestDTO requestDTO) {
        Member member = memberRepository.findById(requestDTO.getMemberId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        WorkoutResult result = WorkoutResult.builder()
            .member(member)
            .activityDetails(requestDTO.getActivityDetails())
            .coachFeedback(requestDTO.getCoachFeedback())
            .loggedDate(requestDTO.getLoggedDate() != null ? requestDTO.getLoggedDate() : LocalDateTime.now())
            .build();
        WorkoutResult saved = workoutResultRepository.save(result);
        return mapToDo(saved);
    }

    @Override
    public List<WorkoutResultResponseDTO> getResultsByMember(Long memberId) {
        return workoutResultRepository.findByMember_UserIdOrderByLoggedDateDesc(memberId).stream().map(this::mapToDo).toList();
    }

    @Override
    public WorkoutResultResponseDTO updateWorkoutResult(Long resultId, WorkoutResultRequestDTO requestDTO) {
        WorkoutResult result = workoutResultRepository.findById(resultId).orElseThrow(() -> new AppException(ErrorCode.RESOUCE_NOT_FOUND));

        if (requestDTO.getActivityDetails() != null) {
            result.setActivityDetails(requestDTO.getActivityDetails());
        }

        if (requestDTO.getCoachFeedback() != null) {
            result.setCoachFeedback(requestDTO.getCoachFeedback());
        }

        return mapToDo(workoutResultRepository.save(result));
    }

    @Override
    @Transactional
    public MemberProgressResponseDTO getMemberProgress(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long attendanceCnt = classAttendanceRepository.countByMember_UserIdAndStatus(memberId, AttendanceStatus.PRESENT);

        List<MemberPlan> plans = memberPlanRepository.findByMember_UserId(memberId);
        int completed = (int) plans.stream().filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus())).count();
        int inProgress = (int) plans.stream().filter(p -> "IN_PROGRESS".equalsIgnoreCase(p.getStatus())).count();

        List<WorkoutResult> results =workoutResultRepository.findByMember_UserIdOrderByLoggedDateDesc(memberId);
        String latestFeedback = results.isEmpty() ? "Chưa có nhận xét nào" : results.get(0).getCoachFeedback();

        return MemberProgressResponseDTO.builder()
            .memberId(memberId)
            .memberName(member.getFirstName() + " " + member.getLastName())
            .trainingGoal(member.getTrainingGoal())
            .totalSessionAttended(attendanceCnt)
            .totalPlansAssigned(plans.size())
            .completedPlans(completed)
            .inProgressPlans(inProgress)
            .lastestCoachFeedback(latestFeedback)
            .recentWorkoutResults(results.stream().limit(5).map(this::mapToDo).toList())
            .build();
    }

    private WorkoutResultResponseDTO mapToDo(WorkoutResult entity) {
        return WorkoutResultResponseDTO.builder()
            .resultId(entity.getResultId())
            .memberId(entity.getMember().getUserId())
            .memberName(entity.getMember().getFirstName() + " " + entity.getMember().getLastName())
            .activityDetails(entity.getActivityDetails())
            .coachFeedback(entity.getCoachFeedback())
            .loggedDate(entity.getLoggedDate())
            .build();
    }
}
