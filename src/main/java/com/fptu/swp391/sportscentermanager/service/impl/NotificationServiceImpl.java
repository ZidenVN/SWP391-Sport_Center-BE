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
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationClassRepository notificationClassRepository;
    private final NotificationAccountRepository notificationAccountRepository;
    private final SportClassRepository sportClassRepository;
    private final MemberRepository memberRepository;
    private final ClassRegistrationRepository classRegistrationRepository;

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NotificationResponseDTO sendNotificationToClass(Long coachId, Long classId, NotificationRequestDTO requestDTO) {
        SportClass sportClass = sportClassRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        String className = sportClass.getSubject() != null ? sportClass.getSubject().getSubjectName() : "Lớp " + classId;

        Notification saved = notificationRepository.save(Notification.builder()
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .targetType("CLASS")
            .sentDate(LocalDateTime.now())
            .sender(userRepository.getReferenceById(coachId))
            .build());

        notificationClassRepository.save(NotificationClass.builder().notification(saved).sportClass(sportClass).build());

        List<Member> members = classRegistrationRepository.findActiveByClassId(classId).stream().map(ClassRegistration::getMember).toList();
        saveRecipients(saved, members, className);
        return toResponse(saved, className, false);

    }

    @Override
    @Transactional
    public NotificationResponseDTO sendNotificationToMember(Long coachId, Long memberId, NotificationRequestDTO requestDTO) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        Notification saved = notificationRepository.save(Notification.builder()
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .targetType("INDIVIDUAL")
            .sentDate(LocalDateTime.now())
            .sender(userRepository.getReferenceById(coachId))
            .build());
        saveRecipients(saved, List.of(member), "Cá nhân");
        return toResponse(saved, member.getFirstName() + " " + member.getLastName(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(Long memberId) {
        List<NotificationAccount> links = notificationAccountRepository.findByUser_UserIdOrderByNotification_SentDateDesc(memberId);

        List<Long> classNoteIds = links.stream().map(NotificationAccount::getNotification).filter(n -> "CLASS".equals(n.getTargetType())).map(Notification::getNotificationId).toList();
        Map<Long, String> classNames = new HashMap<>();
        if (!classNoteIds.isEmpty()) {
            for (NotificationClass nc : notificationClassRepository.findByNotification_NotificationIdIn(classNoteIds)) {
                SportClass c = nc.getSportClass();
                classNames.put(nc.getNotification().getNotificationId(), c.getSubject() != null ? c.getSubject().getSubjectName() : "Lớp học");
            }
        }
        return links.stream().map(na -> {
            Notification n = na.getNotification();
            return toResponse(n, classNames.getOrDefault(n.getNotificationId(), "Cá nhân"), na.getReadAt() != null);
        }).toList();
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
    public long cntUnread(Long memberId) {
        return notificationAccountRepository.countByUser_UserIdAndReadAtIsNull(memberId);
    }

    @Override
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        NotificationAccount link = notificationAccountRepository.findByNotification_NotificationIdAndUser_UserId(notificationId, memberId).orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (link.getReadAt() == null) {
            link.setReadAt(LocalDateTime.now()); // dirty check tự update khi tự commit
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(Long memberId) {
        return notificationAccountRepository.markAllRead(memberId,  LocalDateTime.now());
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
            .category(sender != null ? "STAFF" : "SYSTEM")
            .build();
    }
}
