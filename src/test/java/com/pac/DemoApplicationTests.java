package com.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.pac.controller.GateApiController;
import com.pac.controller.GateApiController.GateRequest;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.service.EmployeeService;

@ExtendWith(MockitoExtension.class)
class DemoApplicationTests {

    @Mock
    private AccessLogRepository accessLogRepo;

    @Mock
    private EmployeeRepository employeeRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private GateApiController controller;

    @Test
    void EmployeeEntry() {
        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Ion")
                .lastName("Palasca")
                .cnp("12345678910111")
                .badgeNumber("EMP1234")
                .divisionId(new UUID(new Random().nextLong(), new Random().nextLong()))
                .bluetoothSecurityCode("456")
                .isAccessActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Configure mocks so the employee is found and access log can be saved
        when(employeeRepo.findByBluetoothSecurityCode(anyString()))
                .thenReturn(Optional.of(emp));
        when(employeeService.canEnter(any())).thenReturn(true);
        when(accessLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = controller.authorize(GateRequest.builder()
                .bluetoothSecurityCode(emp.getBluetoothSecurityCode())
                .accessMethod("bluetooth_esp32")
                .badgeNumber(emp.getBadgeNumber())
                .carPlate("GJ 06 LAY")
                .direction("ENTRY")
                .build());

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
