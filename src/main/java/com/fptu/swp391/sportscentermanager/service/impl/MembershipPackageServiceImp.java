package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
import com.fptu.swp391.sportscentermanager.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageServiceImp implements MembershipPackageService {

    private final MembershipPackageRepository packageRepository;

    @Override
    public List<MembershipPackage> getAllPackage() {
        return packageRepository.findAll();
    }

    @Override
    public MembershipPackage getPackageById(Long id) {
        return packageRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));
    }

    @Override
    public MembershipPackage createPackage(MembershipPackage newPackage) {
        if (newPackage.getStatus() == null || newPackage.getStatus().isBlank()) {
            newPackage.setStatus("ACTIVE");
        }
        return packageRepository.save(newPackage);
    }

    @Override
    public MembershipPackage updatePackage(Long id, MembershipPackage packageDetails) {
        MembershipPackage existingPackage = getPackageById(id);

        existingPackage.setPackageName(packageDetails.getPackageName());
        existingPackage.setDurationDays(packageDetails.getDurationDays());
        existingPackage.setDescription(packageDetails.getDescription());
        existingPackage.setPrice(packageDetails.getPrice());
        if (packageDetails.getStatus() != null && !packageDetails.getStatus().isBlank()) {
            existingPackage.setStatus(packageDetails.getStatus());
        }
        return packageRepository.save(existingPackage);
    }

    @Override
    public void deletePackage(Long id) {
        MembershipPackage existingPackage = getPackageById(id);
        packageRepository.delete(existingPackage);
    }
}
