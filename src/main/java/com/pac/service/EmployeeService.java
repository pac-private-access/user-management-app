package com.pac.service;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pac.dto.EmployeeDto;
import com.pac.entity.AccessSchedule;
import com.pac.entity.Employee;
import com.pac.repository.EmployeeRepository;
import com.pac.repository.ScheduleRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final ScheduleRepository scheduleRepo;

    public EmployeeService(EmployeeRepository employeeRepo, ScheduleRepository scheduleRepo) {
        this.employeeRepo = employeeRepo;
        this.scheduleRepo = scheduleRepo;
    }

    public boolean canEnter(Employee employee) {
        if (employee == null || !employee.isAccessActive()) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        // DB uses 0=Sunday … 6=Saturday, same as JS Date.getDay()
        int currentDay = now.getDayOfWeek().getValue() % 7;
        LocalTime currentTime = now.toLocalTime();
        LocalDate currentDate = now.toLocalDate();

        List<AccessSchedule> schedules = scheduleRepo.findByEmployee(employee);
        for (AccessSchedule s : schedules) {
            boolean dayMatch = s.getDayOfWeek() == null || s.getDayOfWeek() == currentDay;
            boolean timeMatch = !currentTime.isBefore(s.getTimeFrom()) && !currentTime.isAfter(s.getTimeTo());
            boolean dateMatch =
                (s.getValidFrom() == null || !currentDate.isBefore(s.getValidFrom())) &&
                (s.getValidTo()   == null || !currentDate.isAfter(s.getValidTo()));
            if (dayMatch && timeMatch && dateMatch) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public Employee addEmployee(EmployeeDto dto) {
        OffsetDateTime now = OffsetDateTime.now();
        Employee emp = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .cnp(dto.getCnp())
                .photoUrl(dto.getPhotoUrl())
                .badgeNumber(dto.getBadgeNumber())
                .divisionId(dto.getDivisionId())
                .bluetoothSecurityCode(dto.getBluetoothSecurityCode())
                .carPlate(dto.getCarPlate())
                .isAccessActive(dto.isAccessActive())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return employeeRepo.save(emp);
    }

    @Transactional
    public Employee updateEmployee(UUID id, EmployeeDto dto) {
        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        emp.setFirstName(dto.getFirstName());
        emp.setLastName(dto.getLastName());
        emp.setCnp(dto.getCnp());
        emp.setPhotoUrl(dto.getPhotoUrl());
        emp.setBadgeNumber(dto.getBadgeNumber());
        emp.setDivisionId(dto.getDivisionId());
        emp.setBluetoothSecurityCode(dto.getBluetoothSecurityCode());
        emp.setCarPlate(dto.getCarPlate());
        emp.setAccessActive(dto.isAccessActive());
        emp.setUpdatedAt(OffsetDateTime.now());

        return employeeRepo.save(emp);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    public Optional<Employee> getEmployeeById(UUID id) {
        return employeeRepo.findById(id);
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        employeeRepo.deleteById(id);
    }
}
