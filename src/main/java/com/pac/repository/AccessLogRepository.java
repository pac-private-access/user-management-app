package com.pac.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pac.entity.AccessLog;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {
    List<AccessLog> findByEventAtBetween(OffsetDateTime start, OffsetDateTime end);
    List<AccessLog> findByEmployeeIdAndEventAtBetween(UUID employeeId, OffsetDateTime start, OffsetDateTime end);
    Optional<AccessLog> findTopByEmployeeIdOrderByEventAtDesc(UUID employeeId);
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.eventType = :eventType AND a.eventAt >= :start AND a.eventAt < :end")
    long countEntries(@Param("eventType") String eventType, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.eventType = :eventType AND a.isAuthorized = :auth AND a.eventAt >= :start AND a.eventAt < :end")
    long countEntriesByAuth(@Param("eventType") String eventType, @Param("auth") boolean auth, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT ON (employee_id) event_type
            FROM access_logs
            WHERE employee_id IS NOT NULL AND is_authorized = true
            ORDER BY employee_id, event_at DESC
        ) t WHERE event_type = 'entry'
    """, nativeQuery = true)
    long countEmployeesCurrentlyInside();

    @Query(value = """
        SELECT COUNT(*), DATE_TRUNC('day', event_at AT TIME ZONE 'UTC')::date AS day
        FROM access_logs
        WHERE event_at >= :since AND event_type = 'entry'
        GROUP BY day ORDER BY day
    """, nativeQuery = true)
    List<Object[]> countEntriesPerDayAfter(@Param("since") OffsetDateTime since);

    @Query(value = """
        SELECT al.event_at, al.event_type, al.is_authorized,
               COALESCE(e.first_name || ' ' || e.last_name, 'Necunoscut') AS name
        FROM access_logs al
        LEFT JOIN employees e ON e.id = al.employee_id
        ORDER BY al.event_at DESC
        LIMIT 10
    """, nativeQuery = true)
    List<Object[]> findRecentActivity();

    @Query("SELECT a FROM AccessLog a ORDER BY a.eventAt DESC LIMIT 1")
    java.util.Optional<AccessLog> findLatest();

    @Query(value = """
        SELECT al.event_at, al.event_type, al.is_authorized,
               COALESCE(e.last_name || ' ' || e.first_name, 'Necunoscut') AS employee_name,
               COALESCE(d.name, '—') AS division_name,
               al.access_method
        FROM access_logs al
        LEFT JOIN employees e ON e.id = al.employee_id
        LEFT JOIN divisions d ON d.id = e.division_id
        WHERE al.event_at >= :start AND al.event_at < :end
        ORDER BY al.event_at DESC
    """, nativeQuery = true)
    List<Object[]> findReportData(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
