package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MembershipPackageRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.entity.Subject;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MembershipPackageRepository;
import com.fptu.swp391.sportscentermanager.repository.SubjectRepository;
import com.fptu.swp391.sportscentermanager.repository.SubscriptionRepository;
import com.fptu.swp391.sportscentermanager.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipPackageServiceImp implements MembershipPackageService {

    private final MembershipPackageRepository packageRepository;
    private final SubjectRepository subjectRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public List<MembershipPackage> getAllPackage() {
        return packageRepository.findByStatus("ACTIVE");
    }

    @Override
    public MembershipPackage getPackageById(Long id) {
        return packageRepository.findByPackageIdAndStatus(id, "ACTIVE")
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));
    }

    @Override
    @Transactional
    public MembershipPackage createPackage(MembershipPackageRequestDTO request) {
        Subject subject = resolveSubject(request.getPackageType(),
            request.getSubjectId());

        MembershipPackage newPackage = MembershipPackage.builder()
            .packageName(request.getPackageName())
            .description(request.getDescription())
            .price(request.getPrice())
            .durationDays(request.getDurationDays())
            .packageType(request.getPackageType())
            .subject(subject)
            .status(isBlank(request.getStatus()) ? "ACTIVE" : request.getStatus()).build();
        return packageRepository.save(newPackage);
    }

    @Override
    @Transactional
    public MembershipPackage updatePackage(Long id, MembershipPackageRequestDTO request) {
        // Lấy cả gói INACTIVE để Manager mở bán lại được
        MembershipPackage existingPackage = packageRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        Subject subject =  resolveSubject(request.getPackageType(),
            request.getSubjectId());

        boolean coreChanged = existingPackage.getPackageType() != request.getPackageType()
            || !Objects.equals(existingPackage.getDurationDays(), request.getDurationDays())
            || !Objects.equals(subjectIdOf(existingPackage.getSubject()), subjectIdOf(subject));

        if (coreChanged && subscriptionRepository.existsByMembershipPackage_PackageId(id)) {
            throw new AppException(ErrorCode.PACKAGE_IN_USE);
        }

        existingPackage.setPackageName(request.getPackageName());
        existingPackage.setDescription(request.getDescription());
        existingPackage.setPrice(request.getPrice());
        existingPackage.setDurationDays(request.getDurationDays());
        existingPackage.setPackageType(request.getPackageType());
        existingPackage.setSubject(subject);
        if (!isBlank(request.getStatus())) {
            existingPackage.setStatus(request.getStatus());
        }
        return packageRepository.save(existingPackage);
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        MembershipPackage existingPackage = getPackageById(id);
        // Chỉ chuyển INACTIVE: subscription đã mua vẫn cần đọc được gói.
        existingPackage.setStatus("INACTIVE");
        packageRepository.save(existingPackage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getClassDurations(Long subjectId) {
        return packageRepository.findActiveClassDurations(subjectId);
    }

    private Subject resolveSubject(PackageType type, Long subjectId) {
        if (type == PackageType.MEMBERSHIP) {
            if (subjectId != null) {
                throw new AppException(ErrorCode.MEMBERSHIP_PACKAGE_HAS_SUBJECT);
            }
            return null;
        }

        if (type != PackageType.CLASS)
            throw new AppException(ErrorCode.PACKAGE_TYPE_NOT_SUPPORTED);

        if (subjectId == null) {
            throw new AppException((ErrorCode.PACKAGE_SUBJECT_REQUIRED));
        }
        return subjectRepository.findBySubjectIdAndStatus(subjectId, "ACTIVE").orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private Long subjectIdOf(Subject subject) {
        return subject != null ? subject.getSubjectId() : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
