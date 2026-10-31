package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    private Integer maxCapacity;

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

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "day_of_week", length = 30)
    private String dayOfWeek;

    @Column(name = "start_clock")
    private LocalTime startClock;

    @Column(name = "end_clock")
    private LocalTime endClock;

    @Column(name="first_session_at")
    private LocalDateTime firstSessionAt;

}
