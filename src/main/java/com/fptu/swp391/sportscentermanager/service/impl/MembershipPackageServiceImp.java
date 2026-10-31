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
        Set<Subject> subjects = loadActiveSubjects(request.getSubjectIds());
        validateRules(request.getPackageType(), request.getSessionLimit(), subjects);

        MembershipPackage newPackage = MembershipPackage.builder()
            .packageName(request.getPackageName())
            .description(request.getDescription())
            .price(request.getPrice())
            .durationDays(request.getDurationDays())
            .packageType(request.getPackageType())
            .sessionLimit(request.getSessionLimit())
            .subjects(subjects)
            .status(isBlank(request.getStatus()) ? "ACTIVE" : request.getStatus())
            .build();
        return packageRepository.save(newPackage);
    }

    @Override
    @Transactional
    public MembershipPackage updatePackage(Long id, MembershipPackageRequestDTO request) {
        // Lấy cả gói INACTIVE để Manager mở bán lại được
        MembershipPackage existingPackage = packageRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        Set<Subject> subjects = loadActiveSubjects(request.getSubjectIds());
        validateRules(request.getPackageType(), request.getSessionLimit(), subjects);

        // BR-PKG-06: gói đã có người mua thì không được đổi loại, môn, số buổi, hạn dùng
        boolean coreChanged = existingPackage.getPackageType() != request.getPackageType()
            || !Objects.equals(existingPackage.getSessionLimit(), request.getSessionLimit())
            || !Objects.equals(existingPackage.getDurationDays(), request.getDurationDays())
            || !subjectIds(existingPackage.getSubjects()).equals(subjectIds(subjects));
        if (coreChanged && subscriptionRepository.existsByMembershipPackage_PackageId(id)) {
            throw new AppException(ErrorCode.PACKAGE_IN_USE);
        }

        existingPackage.setPackageName(request.getPackageName());
        existingPackage.setDescription(request.getDescription());
        existingPackage.setPrice(request.getPrice());
        existingPackage.setDurationDays(request.getDurationDays());
        existingPackage.setPackageType(request.getPackageType());
        existingPackage.setSessionLimit(request.getSessionLimit());
        existingPackage.setSubjects(subjects);
        if (!isBlank(request.getStatus())) {
            existingPackage.setStatus(request.getStatus());
        }
        return packageRepository.save(existingPackage);
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        MembershipPackage existingPackage = getPackageById(id);
        // Chỉ chuyển INACTIVE, không gọi delete() để Hibernate không xoá dòng package_subjects
        // (subscription đã mua vẫn cần biết gói áp dụng môn nào, BR-PKG-07)
        existingPackage.setStatus("INACTIVE");
        packageRepository.save(existingPackage);
    }

    @Override
    public MembershipPackage getClassDurations(long subjectId) {
        return null;
    }

    // BR-PKG-02, BR-PKG-03, BR-PKG-05, BR-SUBJ-02
    private void validateRules(PackageType type, Integer sessionLimit, Set<Subject> subjects) {
        if (type == PackageType.CLASS) {
            if (subjects.isEmpty()) {
                throw new AppException(ErrorCode.PACKAGE_SUBJECT_REQUIRED);
            }
            return;
        }
        // FREE: không có số buổi, tối đa 1 môn (trống = toàn trung tâm), môn phải OPEN
        if (sessionLimit != null || subjects.size() > 1) {
            throw new AppException(ErrorCode.INVALID_PACKAGE_CONFIG);
        }
        if (subjects.stream().anyMatch(s -> s.getAreaAccess() == AreaAccess.CLASS_ONLY)) {
            throw new AppException(ErrorCode.FREE_PACKAGE_NOT_ALLOWED);
        }
    }

    private Set<Subject> loadActiveSubjects(Set<Long> ids) {
        Set<Subject> result = new HashSet<>();
        if (ids == null) return result;
        for (Long subjectId : ids) {
            result.add(subjectRepository.findBySubjectIdAndStatus(subjectId, "ACTIVE")
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND)));
        }
        return result;
    }

    private Set<Long> subjectIds(Set<Subject> subjects) {
        return subjects.stream().map(Subject::getSubjectId).collect(Collectors.toSet());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
