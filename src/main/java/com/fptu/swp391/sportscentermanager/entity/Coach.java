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
    @Column(name = "speciality")
    @Deprecated
    private String speciality;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coach_subjects",
        joinColumns = @JoinColumn(name = "coach_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id"))
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();
}
