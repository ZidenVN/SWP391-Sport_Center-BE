package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassPlanRepository extends JpaRepository<ClassPlan,Long> {
    List<ClassPlan> findBySportClass_ClassId(Long coachId);

    List<ClassPlan> findByWorkoutPlan_Coach_UserId(Long coachId);
}
