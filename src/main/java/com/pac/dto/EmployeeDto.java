package com.pac.dto;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private String firstName;
    private String lastName;
    private String cnp;
    private String photoUrl;
    private String badgeNumber;
    private UUID divisionId;
    private String bluetoothSecurityCode;
    private String carPlate;
    private boolean isAccessActive;
}
