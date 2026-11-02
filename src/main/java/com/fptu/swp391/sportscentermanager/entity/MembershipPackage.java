package com.fptu.swp391.sportscentermanager.entity;

import com.fptu.swp391.sportscentermanager.enums.PackageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    private Subject subject;

}
