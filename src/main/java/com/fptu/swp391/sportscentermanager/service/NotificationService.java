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

    // BE-16: gửi 1 thông báo hệ thống (in-app) tới nhiều tài khoản, vd khi lớp bị hủy / đổi lịch
    void notifyUsers(List<? extends User> users, String title, String content, String targetType);
}
