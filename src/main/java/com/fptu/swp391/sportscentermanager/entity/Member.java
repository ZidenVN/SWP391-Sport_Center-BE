package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "members")
@PrimaryKeyJoinColumn(name = "member_id")
@NoArgsConstructor @AllArgsConstructor @Data @SuperBuilder
public class Member extends User {
    @Column(name = "training_goal", nullable = false)
    private String trainingGoal;
}
