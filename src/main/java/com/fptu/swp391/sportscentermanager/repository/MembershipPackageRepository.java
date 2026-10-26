package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long>  {
    List<MembershipPackage> findByStatus(String status);
    Optional<MembershipPackage> findByPackageIdAndStatus(Long packageId, String status);
    // BR-PKG-08: còn gói ACTIVE nào áp dụng cho môn này không
    @Query("""
        SELECT COUNT(p) > 0 FROM MembershipPackage p JOIN p.subjects s
        WHERE s.subjectId = :subjectId AND p.status = 'ACTIVE'
        """)
    boolean existsActiveBySubject(@Param("subjectId") Long subjectId);

    // BR-SUBJ-03: còn gói FREE ACTIVE nào của môn này không
    @Query("""
        SELECT COUNT(p) > 0 FROM MembershipPackage p JOIN p.subjects s
        WHERE s.subjectId = :subjectId AND p.status = 'ACTIVE'
          AND p.packageType = com.fptu.swp391.sportscentermanager.enums.PackageType.FREE
        """)
    boolean existsActiveFreeBySubject(@Param("subjectId") Long subjectId);
}
