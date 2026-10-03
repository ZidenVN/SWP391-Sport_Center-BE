package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;

import java.util.List;

public interface MembershipPackageService {
   List<MembershipPackage> getAllPackage();
   MembershipPackage getPackageById(Long id);
   MembershipPackage createPackage(MembershipPackage newPackage);
   MembershipPackage updatePackage(Long id, MembershipPackage packageDetails);
   void deletePackage(Long id);
}
