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
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(unique = true, nullable = false, length = 13)
    private String cnp;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "badge_number", unique = true, nullable = false)
    private String badgeNumber;

    @Column(name = "division_id", nullable = false)
    private UUID divisionId;

    @Column(name = "bluetooth_security_code", unique = true, nullable = false)
    private String bluetoothSecurityCode;

    @Column(name = "car_plate")
    private String carPlate;

    @Column(name = "is_access_active", nullable = false)
    private boolean isAccessActive;

    @Column(name = "access_granted_by")
    private UUID accessGrantedBy;

    @Column(name = "access_granted_by_cnp")
    private String accessGrantedByCnp;

    @Column(name = "access_granted_by_badge")
    private String accessGrantedByBadge;

    @Column(name = "access_granted_at")
    private OffsetDateTime accessGrantedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
