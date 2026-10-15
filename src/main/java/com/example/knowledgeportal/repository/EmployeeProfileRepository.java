package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    Optional<EmployeeProfile> findByUsername(String username);

    Optional<EmployeeProfile> findByUsernameIgnoreCase(String username);

    Optional<EmployeeProfile> findByEmployeeCode(String employeeCode);

    List<EmployeeProfile> findAllByOrderByFullNameAsc();

    List<EmployeeProfile> findByDepartmentIgnoreCase(String department);
}
