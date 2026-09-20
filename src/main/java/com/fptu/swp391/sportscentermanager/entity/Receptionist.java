package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "receptionists")
@PrimaryKeyJoinColumn(name = "receptionist_id")
@NoArgsConstructor @AllArgsConstructor @Data @SuperBuilder
public class Receptionist extends User {
}
