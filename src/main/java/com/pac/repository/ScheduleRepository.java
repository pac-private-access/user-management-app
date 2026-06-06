package com.pac.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pac.entity.AccessSchedule;
import com.pac.entity.Employee;

@Repository
public interface ScheduleRepository extends JpaRepository<AccessSchedule, UUID> {
    List<AccessSchedule> findByEmployee(Employee employee);
}
