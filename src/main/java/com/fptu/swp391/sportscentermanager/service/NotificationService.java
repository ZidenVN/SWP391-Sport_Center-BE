package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.NotificationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.NotificationResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.User;

import java.util.List;

public interface NotificationService {
    NotificationResponseDTO sendNotificationToClass(Long coachId, Long classId, NotificationRequestDTO requestDTO);

    NotificationResponseDTO sendNotificationToMember(Long coachId, Long memberId, NotificationRequestDTO requestDTO);

    List<NotificationResponseDTO> getMyNotifications(Long memberId);

    List<NotificationResponseDTO> getClassNotifications(Long classId);

    // Thông báo tự động do hệ thống sinh ra (đổi lịch lớp, gói sắp hết hạn...), gửi riêng từng user
    void sendSystemNotification(List<? extends User> users, String title, String content);
}
