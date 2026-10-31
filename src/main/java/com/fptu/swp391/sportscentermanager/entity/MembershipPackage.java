package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.fptu.swp391.sportscentermanager.enums.PackageType;
import org.hibernate.annotations.ColumnDefault;

import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packages")

@SQLDelete(sql = "update packages set status = 'INACTIVE' where package_id = ?")

public class MembershipPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "package_name", nullable = false, columnDefinition = "nvarchar(100)")
    private String packageName;

    @Column(name = "duration_days")
    private Integer durationDays;

    private String description;
    private double price;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";

    // BR-PKG-01: FREE = tự tập, CLASS = tự tập + đăng ký lớp
    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 10)
    @ColumnDefault("'CLASS'")
    @Builder.Default
    private PackageType packageType = PackageType.CLASS;

    // BR-PKG-05: null = không giới hạn số buổi. Gói FREE luôn null
    @Column(name = "session_limit")
    @Deprecated
    private Integer sessionLimit;

    // Môn áp dụng. FREE không có môn = gói toàn trung tâm (chỉ khu OPEN)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_subjects",
        joinColumns = @JoinColumn(name = "package_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id"))
    @Builder.Default
    @Deprecated
    private Set<Subject> subjects = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

}
