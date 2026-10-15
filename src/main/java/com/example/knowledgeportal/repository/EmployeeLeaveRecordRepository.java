package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.EmployeeLeaveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeLeaveRecordRepository extends JpaRepository<EmployeeLeaveRecord, Long> {

    Optional<EmployeeLeaveRecord> findByUsername(String username);

    Optional<EmployeeLeaveRecord> findByUsernameIgnoreCase(String username);

    Optional<EmployeeLeaveRecord> findByUsernameAndYear(String username, int year);
}
