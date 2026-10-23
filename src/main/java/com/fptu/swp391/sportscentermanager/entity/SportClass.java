package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    private Integer maxCapacity;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    private String status;

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
