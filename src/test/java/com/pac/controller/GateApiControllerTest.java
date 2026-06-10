package com.pac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.SmartphoneRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeDetailsService;
import com.pac.service.EmployeeService;
import com.pac.service.GatePendingService;
import com.pac.service.JwtService;

@WebMvcTest(GateApiController.class)
class GateApiControllerTest {

    @Autowired MockMvc mvc;

    // Controller dependencies
    @MockitoBean EmployeeRepository employeeRepository;
    @MockitoBean EmployeeService employeeService;
    @MockitoBean AccessLogRepository accessLogRepository;
    @MockitoBean SimpMessagingTemplate messagingTemplate;
    @MockitoBean GatePendingService gatePendingService;

    // JwtAuthenticationFilter dependencies
    @MockitoBean JwtService jwtService;
    @MockitoBean EmployeeDetailsService employeeDetailsService;

    // GlobalModelAttributeAdvice dependencies
    @MockitoBean SmartphoneRepository smartphoneRepository;
    @MockitoBean UserRepository userRepository;

    private Employee employee() {
        return Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Ion").lastName("Popescu")
                .cnp("1234567890123")
                .badgeNumber("EMP0001")
                .divisionId(UUID.randomUUID())
                .bluetoothSecurityCode("BT001")
                .isAccessActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ─── POST /api/gate/authorize ─────────────────────────────────────────────

    @Test
    void authorize_unknownCode_returns404Denied() throws Exception {
        given(employeeRepository.findByBluetoothSecurityCode(anyString()))
                .willReturn(Optional.empty());

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bluetoothSecurityCode\":\"UNKNOWN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.openGate").value(false));
    }

    @Test
    void authorize_emptyCode_returns404WithoutHittingRepo() throws Exception {
        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bluetoothSecurityCode\":\"\"}"))
                .andExpect(status().isNotFound());

        verify(employeeRepository, never()).findByBluetoothSecurityCode(anyString());
    }

    @Test
    void authorize_knownCode_accessGranted_returns200Ok() throws Exception {
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(true);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bluetoothSecurityCode\":\"BT001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.openGate").value(true));
    }

    @Test
    void authorize_knownCode_accessDenied_returns200Denied() throws Exception {
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(false);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bluetoothSecurityCode\":\"BT001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.openGate").value(false));
    }

    @Test
    void authorize_legacyDataField_resolvedCorrectly() throws Exception {
        // ESP32 sends "data" (snake_case device_id is ignored by Jackson — deviceId stays null)
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(true);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"device_id\":\"ESP32_PAC\",\"data\":\"BT001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void authorize_fromMobile_accessGranted_queuesGateOpen() throws Exception {
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(true);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":\"MOBILE_APP\",\"bluetoothSecurityCode\":\"BT001\"}"))
                .andExpect(status().isOk());

        verify(gatePendingService).request();
    }

    @Test
    void authorize_fromEsp32_accessGranted_doesNotQueueGateOpen() throws Exception {
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(true);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":\"ESP32_PAC\",\"bluetoothSecurityCode\":\"BT001\"}"))
                .andExpect(status().isOk());

        verify(gatePendingService, never()).request();
    }

    @Test
    void authorize_accessGranted_broadcastsWebSocketEvent() throws Exception {
        Employee emp = employee();
        given(employeeRepository.findByBluetoothSecurityCode("BT001")).willReturn(Optional.of(emp));
        given(employeeService.canEnter(emp)).willReturn(true);
        given(accessLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/gate/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bluetoothSecurityCode\":\"BT001\"}"))
                .andExpect(status().isOk());

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    // ─── GET /api/gate/poll ───────────────────────────────────────────────────

    @Test
    void poll_whenPending_returnsOpenCommand() throws Exception {
        given(gatePendingService.consume()).willReturn(true);

        mvc.perform(get("/api/gate/poll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("OPEN"));
    }

    @Test
    void poll_whenNoPending_returnsNoneCommand() throws Exception {
        given(gatePendingService.consume()).willReturn(false);

        mvc.perform(get("/api/gate/poll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("NONE"));
    }

    // ─── POST /api/gate/code ──────────────────────────────────────────────────

    @Test
    void code_alwaysReturns200() throws Exception {
        mvc.perform(post("/api/gate/code")
                .contentType(MediaType.TEXT_PLAIN)
                .content("raw esp32 message"))
                .andExpect(status().isOk());
    }
}
