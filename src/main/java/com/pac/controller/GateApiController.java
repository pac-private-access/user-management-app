package com.pac.controller;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pac.entity.AccessLog;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.service.EmployeeService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateApiController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final AccessLogRepository accessLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestBody GateRequest request) {
        Optional<Employee> empOpt = Optional.empty();

        if (request.getBluetoothSecurityCode() != null && !request.getBluetoothSecurityCode().isBlank()) {
            empOpt = employeeRepository.findByBluetoothSecurityCode(request.getBluetoothSecurityCode());
        } else if (request.getBadgeNumber() != null && !request.getBadgeNumber().isBlank()) {
            empOpt = employeeRepository.findByBadgeNumber(request.getBadgeNumber());
        }

        if (empOpt.isEmpty()) {
            accessLogRepository.save(AccessLog.builder()
                    .eventType(request.getDirection())
                    .accessMethod(request.getAccessMethod())
                    .isAuthorized(false)
                    .outOfSchedule(false)
                    .carPlateSeen(request.getCarPlateSeen())
                    .eventAt(OffsetDateTime.now())
                    .syncedToCloud(true)
                    .build());
            return ResponseEntity.status(404).body(new GateResponse("DENIED", "Employee not found"));
        }

        Employee employee = empOpt.get();
        boolean authorized = employeeService.canEnter(employee);
        String eventType = "EXIT".equalsIgnoreCase(request.getDirection()) ? "exit" : "entry";

        accessLogRepository.save(AccessLog.builder()
                .employeeId(employee.getId())
                .eventType(eventType)
                .accessMethod(request.getAccessMethod())
                .isAuthorized(authorized)
                .outOfSchedule(!authorized)
                .carPlateSeen(request.getCarPlateSeen())
                .eventAt(OffsetDateTime.now())
                .syncedToCloud(true)
                .build());

        GateEvent wsMessage = new GateEvent(
                employee.getId().toString(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhotoUrl(),
                employee.getBadgeNumber(),
                employee.getDivisionId().toString(),
                employee.isAccessActive(),
                authorized ? "GRANTED" : "DENIED",
                eventType,
                OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/monitor", wsMessage);

        return ResponseEntity.ok(new GateResponse(
                authorized ? "GRANTED" : "DENIED",
                authorized ? "Access granted" : "Access denied — outside schedule or inactive"
        ));
    }

    @Getter @Setter @NoArgsConstructor
    public static class GateRequest {
        private String bluetoothSecurityCode;
        private String badgeNumber;
        private String direction;    // "ENTRY" or "EXIT"
        private String accessMethod; // "bluetooth_pc" or "bluetooth_esp32"
        private String carPlateSeen;
    }

    @Getter @RequiredArgsConstructor
    public static class GateResponse {
        private final String status;
        private final String message;
    }

    @Getter @RequiredArgsConstructor
    public static class GateEvent {
        private final String employeeId;
        private final String firstName;
        private final String lastName;
        private final String photoUrl;
        private final String badgeNumber;
        private final String divisionId;
        private final boolean isAccessActive;
        private final String result;
        private final String direction;
        private final String timestamp;
    }
}
