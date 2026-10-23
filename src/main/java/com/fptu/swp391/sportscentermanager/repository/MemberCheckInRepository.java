package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MemberCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberCheckInRepository extends JpaRepository<MemberCheckIn, Long> {

    List<MemberCheckIn> findByMember_UserIdOrderByCheckInTimeDesc(Long memberId);

    boolean existsByMember_UserIdAndCheckInTimeBetween(Long memberId, LocalDateTime from, LocalDateTime to);

    long countByMember_UserId(Long memberId);

    @Query("""
        SELECT c FROM MemberCheckIn c
        JOIN FETCH c.member
        LEFT JOIN FETCH c.checkedInBy
        WHERE c.checkInTime BETWEEN :from AND :to
        ORDER BY c.checkInTime DESC
        """)
    List<MemberCheckIn> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
