package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "receptionists")
@PrimaryKeyJoinColumn(name = "receptionist_id")
@NoArgsConstructor @Getter @Setter
@SuperBuilder
public class Receptionist extends User {
}
