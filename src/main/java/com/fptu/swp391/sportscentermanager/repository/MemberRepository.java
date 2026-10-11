package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.Member;
//TODO: Tìm hiểu lí do tại sao lại cần extends JpaRepository trong interface?
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
