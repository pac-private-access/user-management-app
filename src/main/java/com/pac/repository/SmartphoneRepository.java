package com.pac.repository;

import com.pac.entity.Smartphone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmartphoneRepository extends JpaRepository<Smartphone, UUID> {
  List<Smartphone> findByIsActiveFalse();

  List<Smartphone> findByIsActiveTrue();

  Optional<Smartphone> findByEmployeeId(UUID employeeId);
}
