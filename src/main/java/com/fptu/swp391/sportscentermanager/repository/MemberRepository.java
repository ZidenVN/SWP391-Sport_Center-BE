package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Member;
//TODO: Tìm hiểu lí do tại sao lại cần extends JpaRepository trong interface?
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    // 3. Số thành viên có gói tập còn hạn
    @Query("""
        SELECT COUNT(DISTINCT s.member.userId) FROM Subscription s
        WHERE s.status = 'ACTIVE' AND s.endDate >= :currentDate
    """)
    long countActiveSubscriptionMembers(@Param("currentDate") LocalDateTime currentDate);
}
