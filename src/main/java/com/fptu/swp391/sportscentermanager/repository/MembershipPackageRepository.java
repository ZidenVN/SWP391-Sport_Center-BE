package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long>  {
    List<MembershipPackage> findByStatus(String status);
    Optional<MembershipPackage> findByPackageIdAndStatus(Long packageId, String status);
}
