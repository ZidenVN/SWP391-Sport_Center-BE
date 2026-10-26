package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    boolean existsByMember_UserIdAndCheckInTimeBetween(Long memberId, LocalDateTime from, LocalDateTime to);

    @Query("""
        select c from CheckIn c
        join fetch c.member
        left join fetch c.receptionist
        where c.checkInTime >= :from and c.checkInTime < :to
        order by c.checkInTime desc
        """)
    List<CheckIn> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select c from CheckIn c
                join fetch c.member
                        left join fetch c.receptionist
                                where c.member.userId = :memberId
                                        order by c.checkInTime desc
        """)
    List<CheckIn> findByMemberId(@Param("memberId") Long memberId);
}
