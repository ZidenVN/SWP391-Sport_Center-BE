package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
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
        return packageRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy gói tập với ID: " + id));
    }

    @Override
    public MembershipPackage createPackage(MembershipPackage newPackage) {
        return packageRepository.save(newPackage);
    }

    @Override
    public MembershipPackage updatePackage(Long id, MembershipPackage packageDetails) {
        MembershipPackage existingPackage = getPackageById(id);

        existingPackage.setPackageName(packageDetails.getPackageName());
        existingPackage.setDurationDays(packageDetails.getDurationDays());
        existingPackage.setPrice(packageDetails.getPrice());
        existingPackage.setPrice(packageDetails.getPrice());
        return packageRepository.save(existingPackage);
    }

    @Override
    public void deletePackage(Long id) {
        MembershipPackage existingPackage = getPackageById(id);
        packageRepository.delete(existingPackage);
    }
}
