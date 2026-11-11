package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.ClassRegistrationResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.EligibleClassResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberSessionResponseDTO;

import java.util.List;

public interface ClassRegistrationService {

    // Lớp đăng ký / đổi sang được cho 1 gói lớp: cùng môn, cùng thời hạn, còn chỗ, chưa bắt đầu, không trùng lịch
    List<EligibleClassResponseDTO> getEligibleClasses(Long memberId, Long subscriptionId);

    // Lễ tân đăng ký giúp (không cần quiz)
    ClassRegistrationResponseDTO register(Long memberId, Long classId, Long subscriptionId);

    // Member tự đăng ký: phải làm xong quiz trước
    ClassRegistrationResponseDTO registerSelf(Long memberId, Long classId, Long subscriptionId);

    // Hủy đăng ký khi lớp chưa bắt đầu, gói lớp quay về chờ
    ClassRegistrationResponseDTO cancel(Long memberId, Long registrationId);

    // Hội viên tự đổi lớp khi buổi đầu của lớp đang học còn hơn 12 tiếng, không tính lượt
    ClassRegistrationResponseDTO switchSelf(Long memberId, Long registrationId, Long newClassId);

    // Lễ tân đổi lớp: tính 1 lượt / gói lớp khi hội viên đã hết quyền tự đổi; lớp bị dừng thì không tính
    ClassRegistrationResponseDTO switchByReceptionist(Long receptionistId, Long registrationId, Long newClassId);

    // Lịch sử đăng ký (mọi trạng thái)
    List<ClassRegistrationResponseDTO> getHistory(Long memberId);

    // Lịch sắp tới theo buổi (gồm buổi bù, HLV dạy thay)
    List<MemberSessionResponseDTO> getUpcomingSchedule(Long memberId);

    // Lịch đầy đủ theo buổi (cả buổi đã qua) cho trang Lịch tập
    List<MemberSessionResponseDTO> getSchedule(Long memberId);

    // Hàm cho người khác gọi
    void releaseForCancelledClass(Long classId);   // B: huỷ lớp trước khi bắt đầu -> đăng ký CANCELLED, gói về chờ
    void stopRegistrations(Long classId);          // B: dừng lớp giữa khoá -> đăng ký STOPPED
}
