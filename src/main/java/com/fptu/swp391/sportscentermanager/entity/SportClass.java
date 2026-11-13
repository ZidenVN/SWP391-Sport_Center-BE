package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.stereotype.Component;

@Entity
@Table(name = "classes")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
@SQLDelete(sql = "update classes set status = 'INACTIVE' where class_id = ?")
// Lớp bị hủy / đã kết thúc vẫn phải đọc được để giữ lịch sử, chỉ ẩn lớp đã xóa mềm
@SQLRestriction("status != 'INACTIVE'")
public class SportClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    // Mã lớp cho người dùng (vd YOG-0012): sinh 1 lần sau khi có id, không đổi dù sau này đổi môn/ngày
    @Column(name = "class_code", length = 20, unique = true)
    private String classCode;

    public void assignCodeIfMissing() {
        if (classCode != null || classId == null) return;
        String name = subject != null && subject.getSubjectName() != null ? subject.getSubjectName() : "";
        // bỏ dấu tiếng Việt, chỉ giữ chữ cái: "Bơi Lội" -> "BOI"
        String letters = java.text.Normalizer.normalize(name.replace('Đ', 'D').replace('đ', 'd'), java.text.Normalizer.Form.NFD)
            .replaceAll("[^A-Za-z]", "").toUpperCase();
        String prefix = letters.length() >= 3 ? letters.substring(0, 3) : "LOP";
        classCode = prefix + "-" + String.format("%04d", classId);
    }

    // Tên lớp hiển thị trong chữ do BE ghép sẵn (giáo án lớp, check-in): "Yoga YOG-0012"
    public String displayName() {
        String name = subject != null && subject.getSubjectName() != null ? subject.getSubjectName() : "Lớp";
        return name + " " + (classCode != null ? classCode : "#" + classId);
    }

    private Integer maxCapacity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "days_of_week")
    private String daysOfWeek; // e.g. "MON,WED,FRI"

    @Column(name = "start_clock")
    private LocalTime startClock;

    @Column(name = "end_clock")
    private LocalTime endClock;

    @Column(name = "first_session_at")
    private LocalDateTime firstSessionAt;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    private String status;

    @Column(name="image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
}
