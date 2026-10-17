package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.WorkoutResultRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.WorkoutResult;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
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
