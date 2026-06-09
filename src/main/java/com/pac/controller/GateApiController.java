package com.pac.controller;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.pac.entity.AccessLog;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.service.EmployeeService;

import lombok.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gate")
public class GateApiController {

  private final EmployeeRepository employeeRepository;
  private final EmployeeService employeeService;
  private final AccessLogRepository accessLogRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @PostMapping("/authorize")
  public ResponseEntity<GateResponse> authorize(@RequestBody GateRequest request) {
    Optional<Employee> empOpt = Optional.empty();

    if (request.getBluetoothSecurityCode() != null
        && !request.getBluetoothSecurityCode().isEmpty()) {
      empOpt = employeeRepository.findByBluetoothSecurityCode(request.getBluetoothSecurityCode());
    } else if (request.getBadgeNumber() != null && !request.getBadgeNumber().isEmpty()) {
      empOpt = employeeRepository.findByBadgeNumber(request.getBadgeNumber());
    }

    if (empOpt.isEmpty()) {
      return ResponseEntity.status(404).body(new GateResponse("DENIED", "Angajat negăsit", false));
    }

    Employee employee = empOpt.get();
    boolean allowed = employeeService.canEnter(employee);
    boolean outOfSchedule = !allowed && employee.isAccessActive();
    String eventType = "EXIT".equalsIgnoreCase(request.getDirection()) ? "exit" : "entry";
    String accessMethod =
        request.getAccessMethod() != null ? request.getAccessMethod() : "bluetooth_pc";

    AccessLog log =
        AccessLog.builder()
            .employeeId(employee.getId())
            .eventType(eventType)
            .accessMethod(accessMethod)
            .isAuthorized(allowed)
            .outOfSchedule(outOfSchedule)
            .carPlateSeen(request.getCarPlate())
            .eventAt(OffsetDateTime.now())
            .syncedToCloud(true)
            .build();
    accessLogRepository.save(log);

    // Broadcast WebSocket
    GateEvent ws =
        new GateEvent(
            employee.getId().toString(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getBadgeNumber(),
            allowed ? "GRANTED" : "DENIED",
            eventType.toUpperCase(),
            accessMethod,
            OffsetDateTime.now().toString());
    messagingTemplate.convertAndSend("/topic/monitor", ws);

    return ResponseEntity.ok(
        new GateResponse(
            allowed ? "GRANTED" : "DENIED", 
            allowed ? "Acces permis" : "Acces refuzat", 
            allowed));
  }
  
  @PostMapping("/code")
  public ResponseEntity<Void> receive(@RequestBody String message) {
      System.out.println("ESP32: " + message);
      return ResponseEntity.ok().build();
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class GateRequest {
    private String bluetoothSecurityCode;
    private String badgeNumber;
    private String direction;
    private String accessMethod;
    private String carPlate;
  }

  @Getter
  @RequiredArgsConstructor
  public static class GateResponse {
    private final String status;
    private final String message;
    private final boolean openGate;
  }

  @Getter
  @RequiredArgsConstructor
  public static class GateEvent {
    private final String employeeId;
    private final String firstName;
    private final String lastName;
    private final String badgeNumber;
    private final String result;
    private final String direction;
    private final String accessMethod;
    private final String timestamp;
  }
}
