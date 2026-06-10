package com.pac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pac.entity.AccessLog;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.DivisionRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.repository.SmartphoneRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeDetailsService;
import com.pac.service.JwtService;

@WebMvcTest(MobileApiController.class)
class MobileApiControllerTest {

    @Autowired MockMvc mvc;

    // Controller dependencies
    @MockitoBean EmployeeRepository employeeRepository;
    @MockitoBean AccessLogRepository accessLogRepository;
    @MockitoBean ScheduleRepository scheduleRepository;
    @MockitoBean DivisionRepository divisionRepository;

    // JwtAuthenticationFilter dependencies
    @MockitoBean JwtService jwtService;
    @MockitoBean EmployeeDetailsService employeeDetailsService;

    // GlobalModelAttributeAdvice dependencies
    @MockitoBean SmartphoneRepository smartphoneRepository;
    @MockitoBean UserRepository userRepository;

    private Employee employee(UUID id) {
        return Employee.builder()
                .id(id)
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

    // ─── GET /api/mobile/profile/{id} ────────────────────────────────────────

    @Test
    void getProfile_existingEmployee_returns200WithProfileFields() throws Exception {
        UUID id = UUID.randomUUID();
        Employee emp = employee(id);
        given(employeeRepository.findById(id)).willReturn(Optional.of(emp));
        given(scheduleRepository.findByEmployee(emp)).willReturn(List.of());
        given(divisionRepository.findById(emp.getDivisionId())).willReturn(Optional.empty());

        mvc.perform(get("/api/mobile/profile/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(id.toString()))
                .andExpect(jsonPath("$.numeComplet").value("Ion Popescu"))
                .andExpect(jsonPath("$.badgeNumber").value("EMP0001"))
                .andExpect(jsonPath("$.bluetoothCode").value("BT001"))
                .andExpect(jsonPath("$.isAccessActive").value(true));
    }

    @Test
    void getProfile_unknownEmployee_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(employeeRepository.findById(id)).willReturn(Optional.empty());

        mvc.perform(get("/api/mobile/profile/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/mobile/report/{id} ─────────────────────────────────────────

    @Test
    void getReport_noDateParams_returnsCurrentMonthLogs() throws Exception {
        UUID id = UUID.randomUUID();
        given(accessLogRepository.findByEmployeeIdAndEventAtBetween(eq(id), any(), any()))
                .willReturn(List.of());

        mvc.perform(get("/api/mobile/report/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getReport_withDateRange_returnsFilteredLogs() throws Exception {
        UUID id = UUID.randomUUID();
        given(accessLogRepository.findByEmployeeIdAndEventAtBetween(eq(id), any(), any()))
                .willReturn(List.of());

        mvc.perform(get("/api/mobile/report/{id}", id)
                .param("from", "2024-01-01")
                .param("to", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getReport_withLogs_returnsReportEntries() throws Exception {
        UUID id = UUID.randomUUID();
        AccessLog log = AccessLog.builder()
                .id(UUID.randomUUID())
                .employeeId(id)
                .eventType("entry")
                .accessMethod("bluetooth")
                .isAuthorized(true)
                .outOfSchedule(false)
                .eventAt(OffsetDateTime.now())
                .syncedToCloud(true)
                .build();

        given(accessLogRepository.findByEmployeeIdAndEventAtBetween(eq(id), any(), any()))
                .willReturn(List.of(log));

        mvc.perform(get("/api/mobile/report/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("Intrare"))
                .andExpect(jsonPath("$[0].isAuthorized").value(true))
                .andExpect(jsonPath("$[0].synced").value(true));
    }
}
