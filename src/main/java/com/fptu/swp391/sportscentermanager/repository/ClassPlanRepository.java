package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassPlanRepository
    extends JpaRepository<ClassPlan, Long> {

    List<ClassPlan> findBySportClass_ClassId(Long classId);

    List<ClassPlan> findByWorkoutPlan_Coach_UserId(Long coachId);

    @Query("""
        SELECT cp FROM ClassPlan cp
        WHERE cp.sportClass.classId IN (
            SELECT r.sportClass.classId
            FROM ClassRegistration r
            WHERE r.member.userId = :memberId
              AND r.status = 'REGISTERED'
        )
        """)
    List<ClassPlan> findVisibleToMember(
        @Param("memberId") Long memberId
    );
}
