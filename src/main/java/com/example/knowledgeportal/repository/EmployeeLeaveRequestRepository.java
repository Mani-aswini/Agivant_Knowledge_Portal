package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.EmployeeLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLeaveRequestRepository extends JpaRepository<EmployeeLeaveRequest, Long> {

    List<EmployeeLeaveRequest> findByUsernameOrderByStartDateDesc(String username);

    List<EmployeeLeaveRequest> findAllByOrderByStartDateDesc();

    List<EmployeeLeaveRequest> findByStatusOrderByStartDateDesc(String status);
}
