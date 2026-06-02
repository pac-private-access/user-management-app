package com.pac.entity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
    
    @Column(unique = true, nullable = false)
    private String cnp;
    
    @Column(name = "badge_number", nullable = false)
    private String badgeNumber;
    
    @Column(name = "bluetooth_security_code", nullable = false)
    private String bluetoothSecurityCode;
    
    @Column(name = "car_plate")
    private String carPlate;
    
    @Column(name = "division_id", nullable = false)
    private UUID divisionId;
    
    @Column(name = "is_access_active", nullable = false)
    private boolean isAccessActive;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "granted_access_at")
    private LocalDateTime grantedAccessAt;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<EmployeeRole> roles;
}
