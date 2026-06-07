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
@Table(name = "access_logs")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nullable: unknown badge/bluetooth code logs as null
    @Column(name = "employee_id")
    private UUID employeeId;

    // 'entry' or 'exit'
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // 'bluetooth_pc' or 'bluetooth_esp32'
    @Column(name = "access_method", nullable = false)
    private String accessMethod;

    @Column(name = "is_authorized", nullable = false)
    private boolean isAuthorized;

    @Column(name = "out_of_schedule", nullable = false)
    private boolean outOfSchedule;

    @Column(name = "override_by")
    private UUID overrideBy;

    @Column(name = "car_plate_seen")
    private String carPlateSeen;

    @Column(name = "event_at", nullable = false)
    private OffsetDateTime eventAt;

    @Column(name = "synced_to_cloud", nullable = false)
    private boolean syncedToCloud;
}
