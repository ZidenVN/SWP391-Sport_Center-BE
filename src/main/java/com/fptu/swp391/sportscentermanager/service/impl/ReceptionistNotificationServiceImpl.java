package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.ReceptionistNotificationRequestDTO;
import com.fptu.swp391.sportscentermanager.entity.Member;
import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.enums.UserStatus;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.MemberRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import com.fptu.swp391.sportscentermanager.service.ReceptionistNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceptionistNotificationServiceImpl implements ReceptionistNotificationService {

    private static final String TARGET_TYPE = "STAFF_MESSAGE";

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public int send(Long senderId, ReceptionistNotificationRequestDTO dto) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<Member> recipients = switch (dto.getTargetMode().trim().toUpperCase()) {
            case "MEMBERS" -> findSelectedMembers(dto.getMemberIds());
            case "ALL_ACTIVE" -> memberRepository.findByStatus(UserStatus.ACTIVE);
            default -> throw new AppException(ErrorCode.NOTIFICATION_INVALID_TARGET);
        };
        if (recipients.isEmpty()) {
            throw new AppException(ErrorCode.NOTIFICATION_NO_RECIPIENT);
        }

        // Không tự ghi bảng notifications: đi qua service của A để có người gửi + được đẩy realtime
        notificationService.notifyUsers(recipients, dto.getTitle().trim(), dto.getContent().trim(), TARGET_TYPE, sender);
        return recipients.size();
    }

    // Mọi id phải là member tồn tại và đang ACTIVE, sai 1 id thì chặn cả lần gửi
    private List<Member> findSelectedMembers(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new AppException(ErrorCode.NOTIFICATION_NO_RECIPIENT);
        }
        Set<Long> ids = new LinkedHashSet<>(memberIds);
        if (ids.contains(null)) {
            throw new AppException(ErrorCode.NOTIFICATION_MEMBER_INVALID);
        }

        List<Member> members = memberRepository.findAllById(ids);
        boolean invalid = members.size() != ids.size()
            || members.stream().anyMatch(m -> m.getStatus() != UserStatus.ACTIVE);
        if (invalid) {
            throw new AppException(ErrorCode.NOTIFICATION_MEMBER_INVALID);
        }
        return members;
    }
}
