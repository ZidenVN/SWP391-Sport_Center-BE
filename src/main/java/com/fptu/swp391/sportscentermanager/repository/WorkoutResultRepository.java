package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.WorkoutResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutResultRepository
extends JpaRepository<WorkoutResult,Long> {
    List<WorkoutResult> findByMember_UserIdOrderByLoggedDateDesc(Long memberId);
}
