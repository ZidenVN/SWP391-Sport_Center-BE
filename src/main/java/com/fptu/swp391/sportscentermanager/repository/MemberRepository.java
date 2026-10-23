package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Member;
//TODO: Tìm hiểu lí do tại sao lại cần extends JpaRepository trong interface?
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
    @Query("""
        SELECT m FROM Member m
        WHERE m.role.roleName = 'MEMBER'
          AND (
               LOWER(m.firstName) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(CONCAT(m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR m.phone LIKE CONCAT('%', :kw, '%')
          )
        ORDER BY m.userId DESC
        """)
    List<Member> search(@Param("kw") String keyword);


    // 1. Tổng số thành viên
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role.roleName = 'MEMBER'")
    long countTotalMembers();

    // 2. Số thành viên mới tạo trong khoảng thời gian
    @Query("""
        SELECT COUNT(m) FROM Member m
        WHERE m.role.roleName = 'MEMBER' AND m.createdAt BETWEEN :startDate AND :endDate
    """)
    long countNewMembersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 3. Số thành viên có gói tập đang hiệu lực tại thời điểm currentDate
    // (BE-22: cùng định nghĩa với SubscriptionRepository.findCurrent, gói gia hạn nối tiếp chưa bắt đầu không tính)
    @Query("""
        SELECT COUNT(DISTINCT s.member.userId) FROM Subscription s
        WHERE s.status = 'ACTIVE' AND s.startDate <= :currentDate AND s.endDate > :currentDate
    """)
    long countActiveSubscriptionMembers(@Param("currentDate") LocalDateTime currentDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.userId =  :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
    // 4. BE-22: số thành viên mới theo từng ngày (growthTrend)
    @Query(value = """
        SELECT DATE_FORMAT(u.create_at, '%Y-%m-%d') AS dayLabel, COUNT(*) AS newMembers
        FROM users u
        JOIN members m ON m.member_id = u.user_id
        WHERE u.create_at BETWEEN :start AND :end
        GROUP BY DATE_FORMAT(u.create_at, '%Y-%m-%d')
        ORDER BY dayLabel ASC
    """, nativeQuery = true)
    List<Object[]> countNewMembersByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
