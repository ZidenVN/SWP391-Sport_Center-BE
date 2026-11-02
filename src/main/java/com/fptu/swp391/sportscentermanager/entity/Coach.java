package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "coaches")
@PrimaryKeyJoinColumn(name = "coach_id")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter
@SuperBuilder
public class Coach extends User {

    /** @deprecated Thay bằng {@link #subjects}. Chỉ giữ để tương thích dữ liệu cũ, hệ thống tự ghi tên các môn vào đây. */
    @Deprecated
    @Column(name = "speciality")
    private String speciality;

    // Chuyên môn: mỗi HLV có ít nhất 1 môn, không giới hạn số môn
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coach_subjects",
        joinColumns = @JoinColumn(name = "coach_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id"))
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();
}
