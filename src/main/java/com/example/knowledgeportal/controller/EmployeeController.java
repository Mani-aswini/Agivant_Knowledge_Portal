package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.CreateEmployeeRequest;
import com.example.knowledgeportal.dto.EmployeeDetailResponse;
import com.example.knowledgeportal.dto.EmployeeManagementResponse;
import com.example.knowledgeportal.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Creates a new employee with login credentials and initial leave balances.
     * Restricted to Admin & HR.
     */
    @PostMapping
    public ResponseEntity<EmployeeDetailResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        EmployeeDetailResponse response = employeeService.createEmployee(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves aggregated employee directory and leave overview for Admin & HR.
     */
    @GetMapping
    public ResponseEntity<EmployeeManagementResponse> getEmployeesOverview(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        EmployeeManagementResponse response = employeeService.getEmployeesOverview(department, search, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the logged-in employee's own profile and leave balances.
     */
    @GetMapping("/me")
    public ResponseEntity<EmployeeDetailResponse> getCurrentEmployeeDetails(Authentication authentication) {
        EmployeeDetailResponse response = employeeService.getCurrentEmployeeDetails(authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details for a specific employee.
     */
    @GetMapping("/{username}")
    public ResponseEntity<EmployeeDetailResponse> getEmployeeDetails(
            @PathVariable String username,
            Authentication authentication) {
        EmployeeDetailResponse response = employeeService.getEmployeeDetails(username, authentication);
        return ResponseEntity.ok(response);
    }
}
