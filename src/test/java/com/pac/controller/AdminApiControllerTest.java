package com.pac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pac.dto.EmployeeDto;
import com.pac.entity.Employee;
import com.pac.repository.AccessLogRepository;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;
import com.pac.repository.SmartphoneRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeDetailsService;
import com.pac.service.EmployeeService;
import com.pac.service.JwtService;

@WebMvcTest(AdminApiController.class)
class AdminApiControllerTest {

    @Autowired MockMvc mvc;

    // Controller dependencies
    @MockitoBean EmployeeService employeeService;
    @MockitoBean EmployeeRepository employeeRepository;
    @MockitoBean ScheduleRepository scheduleRepository;
    @MockitoBean AccessLogRepository accessLogRepository;

    // JwtAuthenticationFilter dependencies
    @MockitoBean JwtService jwtService;
    @MockitoBean EmployeeDetailsService employeeDetailsService;

    // GlobalModelAttributeAdvice dependencies
    @MockitoBean SmartphoneRepository smartphoneRepository;
    @MockitoBean UserRepository userRepository;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DIVISION_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SCHEDULE_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static final String EMPLOYEE_JSON =
            "{\"firstName\":\"Ion\",\"lastName\":\"Popescu\"," +
            "\"cnp\":\"1234567890123\",\"badgeNumber\":\"EMP0001\"," +
            "\"divisionId\":\"" + DIVISION_ID + "\"," +
            "\"bluetoothSecurityCode\":\"BT001\",\"isAccessActive\":true}";

    private Employee employee() {
        return Employee.builder()
                .id(EMPLOYEE_ID)
                .firstName("Ion").lastName("Popescu")
                .cnp("1234567890123")
                .badgeNumber("EMP0001")
                .divisionId(DIVISION_ID)
                .bluetoothSecurityCode("BT001")
                .isAccessActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ─── Security ─────────────────────────────────────────────────────────────

    @Test
    void adminEndpoints_withoutAuth_returns401() throws Exception {
        mvc.perform(get("/api/admin/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUARD")
    void adminEndpoints_withGuardRole_returns403() throws Exception {
        mvc.perform(get("/api/admin/employees"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/admin/employees ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllEmployees_returns200WithList() throws Exception {
        given(employeeService.getAllEmployees()).willReturn(List.of(employee()));

        mvc.perform(get("/api/admin/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].badgeNumber").value("EMP0001"));
    }

    // ─── GET /api/admin/employees/{id} ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEmployeeById_existing_returns200() throws Exception {
        given(employeeService.getEmployeeById(EMPLOYEE_ID)).willReturn(Optional.of(employee()));

        mvc.perform(get("/api/admin/employees/{id}", EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Ion"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEmployeeById_nonExisting_returns404() throws Exception {
        given(employeeService.getEmployeeById(EMPLOYEE_ID)).willReturn(Optional.empty());

        mvc.perform(get("/api/admin/employees/{id}", EMPLOYEE_ID))
                .andExpect(status().isNotFound());
    }

    // ─── POST /api/admin/employees ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_validBody_returns200WithEmployee() throws Exception {
        given(employeeService.addEmployee(any(EmployeeDto.class))).willReturn(employee());

        mvc.perform(post("/api/admin/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPLOYEE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.badgeNumber").value("EMP0001"));
    }

    // ─── PUT /api/admin/employees/{id} ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEmployee_existing_returns200() throws Exception {
        given(employeeService.updateEmployee(eq(EMPLOYEE_ID), any(EmployeeDto.class)))
                .willReturn(employee());

        mvc.perform(put("/api/admin/employees/{id}", EMPLOYEE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPLOYEE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEmployee_nonExisting_returns404() throws Exception {
        given(employeeService.updateEmployee(eq(EMPLOYEE_ID), any(EmployeeDto.class)))
                .willThrow(new IllegalArgumentException("Employee not found"));

        mvc.perform(put("/api/admin/employees/{id}", EMPLOYEE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPLOYEE_JSON))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/admin/employees/{id} ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployee_returns204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(EMPLOYEE_ID);

        mvc.perform(delete("/api/admin/employees/{id}", EMPLOYEE_ID))
                .andExpect(status().isNoContent());
    }

    // ─── POST /api/admin/schedules ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSchedule_validRequest_returns200() throws Exception {
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee()));
        given(scheduleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/admin/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + EMPLOYEE_ID + "\",\"dayOfWeek\":1," +
                        "\"timeFrom\":\"08:00\",\"timeTo\":\"17:00\"}"))
                .andExpect(status().isOk());
    }

    // ─── GET /api/admin/schedules/employee/{id} ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEmployeeSchedules_returns200() throws Exception {
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee()));
        given(scheduleRepository.findByEmployee(any())).willReturn(List.of());

        mvc.perform(get("/api/admin/schedules/employee/{id}", EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ─── DELETE /api/admin/schedules/{id} ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSchedule_returns204() throws Exception {
        doNothing().when(scheduleRepository).deleteById(SCHEDULE_ID);

        mvc.perform(delete("/api/admin/schedules/{id}", SCHEDULE_ID))
                .andExpect(status().isNoContent());
    }

    // ─── GET /api/admin/reports/logs ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAccessLogs_withDateRange_returns200WithList() throws Exception {
        given(accessLogRepository.findByEventAtBetween(any(), any())).willReturn(List.of());

        mvc.perform(get("/api/admin/reports/logs")
                .param("start", "2024-01-01T00:00:00+00:00")
                .param("end",   "2024-02-01T00:00:00+00:00"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAccessLogs_withEmployeeFilter_returns200() throws Exception {
        given(accessLogRepository.findByEmployeeIdAndEventAtBetween(eq(EMPLOYEE_ID), any(), any()))
                .willReturn(List.of());

        mvc.perform(get("/api/admin/reports/logs")
                .param("start",      "2024-01-01T00:00:00+00:00")
                .param("end",        "2024-02-01T00:00:00+00:00")
                .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk());
    }
}
