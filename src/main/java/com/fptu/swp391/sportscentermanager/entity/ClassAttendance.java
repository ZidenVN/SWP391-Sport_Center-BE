package com.fptu.swp391.sportscentermanager.entity;

import com.fptu.swp391.sportscentermanager.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.ISBN;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_attendances")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ClassAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SportClass sportClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "attendance_date", nullable = false)
    private LocalDateTime attendanceDate;

    @Column(name = "status", nullable = false, columnDefinition = "NVARCHAR(20)")
    private AttendanceStatus status;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;

    @CreationTimestamp
    @Column(name = "marked_at")
    private LocalDateTime markedAt;

}
