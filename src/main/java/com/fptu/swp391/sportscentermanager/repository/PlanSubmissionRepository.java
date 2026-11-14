package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.PlanSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanSubmissionRepository
    extends JpaRepository<PlanSubmission, Long> {

    List<PlanSubmission>
    findByMemberPlan_MemberPlanIdOrderByAttemptAsc(Long id);
}
