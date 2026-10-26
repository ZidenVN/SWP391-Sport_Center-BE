package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;

import java.util.List;
import com.fptu.swp391.sportscentermanager.dto.MembershipPackageRequestDTO;
public interface MembershipPackageService {
   List<MembershipPackage> getAllPackage();
   MembershipPackage getPackageById(Long id);
    MembershipPackage createPackage(MembershipPackageRequestDTO request);
    MembershipPackage updatePackage(Long id, MembershipPackageRequestDTO request);
   void deletePackage(Long id);
}
