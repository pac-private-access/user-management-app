package com.pac.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pac.entity.AccessLog;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {
    List<AccessLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<AccessLog> findByEmployeeIdAndTimestampBetween(UUID employeeId, LocalDateTime start, LocalDateTime end);
}
