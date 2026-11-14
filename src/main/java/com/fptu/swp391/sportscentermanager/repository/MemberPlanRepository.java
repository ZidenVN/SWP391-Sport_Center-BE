package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MemberPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPlanRepository extends JpaRepository<MemberPlan,Long> {
    List<MemberPlan> findByMember_UserId(Long memberId);

    List <MemberPlan> findByWorkoutPlan_Coach_UserId(Long coachId);
    @org.springframework.data.jpa.repository.Lock(
        jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
    )
    @org.springframework.data.jpa.repository.Query(
        "select mp from MemberPlan mp where mp.memberPlanId = :id"
    )
    java.util.Optional<MemberPlan> findLocked(
        @org.springframework.data.repository.query.Param("id") Long id
    );
}
