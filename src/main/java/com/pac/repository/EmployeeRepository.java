package com.pac.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pac.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByBluetoothSecurityCode(String bluetoothSecurityCode);
    Optional<Employee> findByBadgeNumber(String badgeNumber);
    List<Employee> findByFirstNameAndLastNameContaining(String firstName, String LastName);
    long countByIsAccessActive(boolean isAccessActive);
}
