package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MemberPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPlanRepository extends JpaRepository<MemberPlan,Long> {
    List<MemberPlan> findByMember_UserId(Long memberId);

    List <MemberPlan> findByWorkoutPlan_Coach_UserId(Long coachId);
}
