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

    @Query("""
        SELECT COUNT(c) > 0 FROM SportClass c WHERE c.room.roomId = :roomId AND c.classId<> :excludeClassId AND c.startTime < :endTime AND :startTime < c.endTime""")
    boolean existsRoomConflict(@Param("roomId") Long roomId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("excludeClassId") Long excludeClassId);

    @Query("""
        SELECT COUNT(c) > 0 FROM SportClass c
        WHERE c.coach.userId = :coachId
        AND c.classId <> :excludeClassId
        AND c.startTime < :endTime
        AND :startTime < c.endTime
        """)
    boolean existsCoachConflict(@Param("coachId") Long coachId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeClassId") Long excludeClassId);

    boolean existsByRoom_RoomIdAndMaxCapacityGreaterThan(Long roomId, Integer capacity);
}
