package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;

import java.util.List;

public interface ClassRegistrationService {

    // Đăng ký lớp (Member tự đăng ký hoặc Lễ tân đăng ký giúp)
    ClassRegistrationResponseDTO register(Long memberId, Long classId);

    // Member tự đăng ký: phải làm xong quiz trước (Lễ tân đăng ký giúp thì không cần)
    ClassRegistrationResponseDTO registerSelf(Long memberId, Long classId);

    // Hủy đăng ký
    ClassRegistrationResponseDTO cancel(Long memberId, Long registrationId);

    // Lịch sử đăng ký (cả REGISTERED và CANCELLED)
    List<ClassRegistrationResponseDTO> getHistory(Long memberId);

    // Lịch cá nhân sắp tới
    List<ClassRegistrationResponseDTO> getUpcomingSchedule(Long memberId);
}
