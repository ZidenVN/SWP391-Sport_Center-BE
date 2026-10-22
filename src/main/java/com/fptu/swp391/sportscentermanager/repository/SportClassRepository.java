package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.SportClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SportClassRepository extends JpaRepository<SportClass, Long> {

    // Khóa dòng của lớp trong lúc đăng ký: 2 người bấm cùng lúc sẽ xếp hàng,
    // không thể cùng lọt vào chỗ cuối cùng. Phải gọi trong @Transactional.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SportClass c WHERE c.classId = :id")
    Optional<SportClass> findByIdForUpdate(@Param("id") Long id);
    List<SportClass> findByCoach_UserIdOrderByStartTimeAsc(Long coachId);

    List<SportClass> findByCoach_UserIdAndStartTimeBetweenOrderByStartTimeAsc(Long coachId, LocalDateTime start, LocalDateTime end);
}
