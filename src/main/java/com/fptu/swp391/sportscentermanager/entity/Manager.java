package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "managers")
@PrimaryKeyJoinColumn(name = "manager_id")
@NoArgsConstructor @AllArgsConstructor @Data @SuperBuilder
public class Manager extends User {

}
