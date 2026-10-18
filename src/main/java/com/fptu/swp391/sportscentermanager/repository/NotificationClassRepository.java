package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.NotificationClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationClassRepository extends JpaRepository<NotificationClass,Long> {
    List<NotificationClass> findBySportClass_ClassIdOrderByNotification_SentDateDesc(Long classId);
    List<NotificationClass> findBySportClass_ClassIdIn(List<Long> classId);
}
