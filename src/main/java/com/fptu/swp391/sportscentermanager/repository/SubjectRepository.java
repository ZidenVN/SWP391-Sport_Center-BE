package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fptu.swp391.sportscentermanager.entity.Subject;

import java.util.List;
import java.util.Optional;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
public interface SubjectRepository extends JpaRepository<Subject,Long> {
    List<Subject> findByStatus(String status);
    Optional<Subject> findBySubjectIdAndStatus(Long subjectId, String status);
    // Gói FREE toàn trung tâm = mọi môn OPEN đang hoạt động
    List<Subject> findByStatusAndAreaAccess(String status, AreaAccess areaAccess);
}
