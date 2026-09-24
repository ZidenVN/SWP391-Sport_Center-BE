package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pakages")
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
}
