package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    long countBySportClass_ClassIdAndStatus(Long classId, String status);
    List<ClassEnrollment> findBySportClass_ClassId(Long classId);
}
