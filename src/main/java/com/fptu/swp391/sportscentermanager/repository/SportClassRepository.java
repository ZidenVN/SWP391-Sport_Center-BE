package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.SportClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SportClassRepository extends JpaRepository<SportClass, Long> {
    List<SportClass> findByCoach_UserIdOrderByScheduleTimeAsc(Long coachId);

    List<SportClass> findByCoach_UserIdAndScheduleTimeBetweenOrderByScheduleTimeAsc(Long coachId, LocalDateTime start, LocalDateTime end);
}
