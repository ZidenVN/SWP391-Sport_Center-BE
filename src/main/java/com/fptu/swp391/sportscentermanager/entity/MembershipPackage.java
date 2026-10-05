package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packages")

@SQLDelete(sql = "update packages set status = 'INACTIVE' where package_id = ?")

@SQLRestriction("status = 'ACTIVE'")
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

}
