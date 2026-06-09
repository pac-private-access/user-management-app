package com.pac.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "smartphones")
public class Smartphone {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "employee_id", unique = true, nullable = false)
  private UUID employeeId;

  @Column(name = "puk_code", unique = true, nullable = false)
  private String pukCode;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "registered_at", nullable = false)
  private OffsetDateTime registeredAt;
}
