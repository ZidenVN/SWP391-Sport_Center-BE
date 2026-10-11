package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.ISBN;

import java.time.LocalDateTime;

@Entity
@Table("class_attendances")
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
    @JoinColumn(name = "")
    private Long memberId;
    private LocalDateTime attendanceDate;
    private String status;
    private LocalDateTime markedAt;

}
