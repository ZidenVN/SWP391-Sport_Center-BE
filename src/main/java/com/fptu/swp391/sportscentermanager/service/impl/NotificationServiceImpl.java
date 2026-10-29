package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.NotificationRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.NotificationResponseDTO;
import com.fptu.swp391.sportscentermanager.entity.*;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.event.NotificationCreatedEvent;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.repository.*;
import com.fptu.swp391.sportscentermanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationClassRepository notificationClassRepository;
    private final NotificationAccountRepository notificationAccountRepository;
    private final SportClassRepository sportClassRepository;
    private final MemberRepository memberRepository;
    private final ClassRegistrationRepository classRegistrationRepository;

    private final ClassRegistrationRepository classRegistrationClassRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public NotificationResponseDTO sendNotificationToClass(Long coachId, Long classId, NotificationRequestDTO requestDTO) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Notification notification = Notification.builder()
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .targetType("CLASS")
            .sentDate(LocalDateTime.now())
            .build();

        Notification savedNotification = notificationRepository.save(notification);

        NotificationClass notificationClass = NotificationClass.builder()
            .notification(savedNotification)
            .sportClass(sportClass)
            .build();
        notificationClassRepository.save(notificationClass);

        return NotificationResponseDTO.builder()
            .notificationId(savedNotification.getNotificationId())
            .title(savedNotification.getTitle())
            .content(savedNotification.getContent())
            .targetType("CLASS")
            .sendDate(savedNotification.getSentDate())
            .targetName(sportClass.getSubject() != null ? sportClass.getSubject().getSubjectName() : "Lớp " + classId).build();
    }

    @Override
    public NotificationResponseDTO sendNotificationToMember(Long coachId, Long memberId, NotificationRequestDTO requestDTO) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        Notification notification = Notification.builder()
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .targetType("INDIVIDUAL")
            .sentDate(LocalDateTime.now())
            .build();

        Notification savedNotification = notificationRepository.save(notification);

        NotificationAccount notificationAccount = NotificationAccount.builder()
            .notification(savedNotification)
            .user(member)
            .build();
        notificationAccountRepository.save(notificationAccount);

        return NotificationResponseDTO.builder()
            .notificationId(savedNotification.getNotificationId())
            .title(savedNotification.getTitle())
            .content(savedNotification.getContent())
            .targetType("INDIVIDUAL")
            .sendDate(savedNotification.getSentDate())
            .targetName(member.getFirstName() + " " + member.getLastName())
            .build();

    }

    @Override
    public List<NotificationResponseDTO> getMyNotifications(Long memberId) {
        List<NotificationResponseDTO> result = new ArrayList<>();

        // 1. Lấy thông báo gửi riêng cho cá nhân
        List<NotificationAccount> accountNotes = notificationAccountRepository.findByUser_UserIdOrderByNotification_SentDateDesc(memberId);
        for (NotificationAccount na : accountNotes) {
            Notification n = na.getNotification();
            result.add(NotificationResponseDTO.builder()
                .notificationId(n.getNotificationId())
                .title(n.getTitle())
                .content(n.getContent())
                .targetType(n.getTargetType())
                .sendDate(n.getSentDate())
                .targetName("Cá nhân")
                .build());
        }

        List<Long> classIds = classRegistrationRepository.findActiveClassIdsByMember(memberId);
        if (!classIds.isEmpty()) {
            List<NotificationClass> classNotes = notificationClassRepository.findBySportClass_ClassIdIn(classIds);
            for (NotificationClass nc : classNotes) {
                Notification n = nc.getNotification();
                result.add(NotificationResponseDTO.builder()
                    .notificationId(n.getNotificationId())
                    .title(n.getTitle())
                    .content(n.getContent())
                    .targetType(n.getTargetType())
                    .sendDate(n.getSentDate())
                    .targetName(nc.getSportClass().getSubject() != null ? nc.getSportClass().getSubject().getSubjectName() : "Lớp học")
                    .build());
            }
        }


        result.sort(Comparator.comparing(NotificationResponseDTO::getSendDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    @Transactional
    public void notifyUsers(List<? extends User> users, String title, String content, String targetType) {
        notifyUsers(users, title, content, targetType, null);
    }

    @Override
    @Transactional
    public void notifyUsers(List<? extends User> users, String title, String content, String targetType, User sender) {
        if (users == null || users.isEmpty()) {
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder()
            .title(title)
            .content(content)
            .targetType(targetType)
            .sentDate(LocalDateTime.now())
            .sender(sender)
            .build());
        saveRecipients(notification, users, "Cá nhân");
    }


    @Override
    @Transactional
    public List<NotificationResponseDTO> getClassNotifications(Long classId) {
        return notificationClassRepository.findBySportClass_ClassIdOrderByNotification_SentDateDesc(classId)
            .stream()
            .map(nc -> NotificationResponseDTO.builder()
                .notificationId(nc.getNotification().getNotificationId())
                .title(nc.getNotification().getTitle())
                .content(nc.getNotification().getContent())
                .targetType("CLASS")
                .sendDate(nc.getNotification().getSentDate())
                .targetName("Lớp " + classId)
                .build())
            .toList();
    }

    private void saveRecipients(Notification notification, List<? extends User> users, String targetName) {
        if (users.isEmpty()) return;
        List<NotificationAccount> links = new ArrayList<>();
        for (User u : users) {
            links.add(NotificationAccount.builder()
                .notification(notification)
                .user(u)
                .build());
        }
        notificationAccountRepository.saveAll(links);
        List<String> emails = users.stream().map(User::getEmail).toList();
        eventPublisher.publishEvent(new NotificationCreatedEvent(toResponse(notification, targetName, false), emails));
    }

    private NotificationResponseDTO toResponse(Notification n, String targetName, boolean read) {
        User sender = n.getSender();
        return NotificationResponseDTO.builder()
            .notificationId(n.getNotificationId())
            .title(n.getTitle())
            .content(n.getContent())
            .targetType(n.getTargetType())
            .sendDate(n.getSentDate())
            .targetName(targetName)
            .read(read)
            .senderName(sender != null ? sender.getFirstName() + " " + sender.getLastName() : null)
            .category(sender != null ? "STAFF" :"SYSTEM")
            .build();
    }
}
