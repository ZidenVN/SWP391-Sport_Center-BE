package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fptu.swp391.sportscentermanager.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject,Long> {
    List<Subject> findByStatus(String status);
    Optional<Subject> findBySubjectIdAndStatus(Long subjectId, String status);
}
