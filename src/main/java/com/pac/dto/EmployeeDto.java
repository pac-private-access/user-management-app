package com.pac.dto;

import java.util.Set;
import java.util.UUID;

import com.pac.entity.EmployeeRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private UUID id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String cnp;
    private String badgeNumber;
    private String bluetoothSecurityCode;
    private String carPlate;
    private UUID divisionId;
    private boolean isAccessActive;
    private Set<EmployeeRole> roles;
}
