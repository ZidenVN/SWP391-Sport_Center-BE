package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
    private LocalDateTime scheduleTime;
    // OPENING | CANCELLED | COMPLETED | INACTIVE (xem enums.ClassStatus)
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
