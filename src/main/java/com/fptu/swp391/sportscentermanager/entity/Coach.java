package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "coaches")
@PrimaryKeyJoinColumn(name = "coach_id")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter
@SuperBuilder
public class Coach extends User {
    @Column(name = "speciality", nullable = false)
    private String speciality;
}
