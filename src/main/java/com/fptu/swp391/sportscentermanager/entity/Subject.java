package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
import org.hibernate.annotations.ColumnDefault;
@Entity
@Table(name = "subjects")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder

@SQLDelete(sql = "update subjects set status = 'INACTIVE' where subject_id = ?")

public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_name", columnDefinition = "TEXT")
    private String subjectName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";

    // BR-SUBJ-01: OPEN = khu chung, CLASS_ONLY = khu đặc thù chỉ cho gói CLASS
    @Enumerated(EnumType.STRING)
    @Column(name = "area_access", nullable = false, length = 20)
    @ColumnDefault("'OPEN'")
    @Builder.Default
    private AreaAccess areaAccess = AreaAccess.OPEN;
}
