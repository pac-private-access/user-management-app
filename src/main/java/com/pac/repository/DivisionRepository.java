package com.pac.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pac.entity.Division;

@Repository
public interface DivisionRepository extends JpaRepository<Division, UUID> {
}
