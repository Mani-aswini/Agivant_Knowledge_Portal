package com.example.knowledgeportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new company employee.
 * Admin or HR specifies login credentials (username, password, role)
 * and corporate profile details (department, designation, initial leave allocation).
 */
public class CreateEmployeeRequest {

    @NotBlank(message = "Username is required for employee login")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required for employee login")
    @Size(min = 4, max = 100, message = "Password must be at least 4 characters")
    private String password;

    @NotBlank(message = "Full Name is required")
    @Size(max = 100, message = "Full Name cannot exceed 100 characters")
    private String fullName;

    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Designation/Job Title is required")
    private String designation;

    private String employeeCode;

    private String role; // ROLE_EMPLOYEE, ROLE_HR, ROLE_ADMIN

    private String location;

    private String reportingManager;

    // Optional Initial Leave Allocations for Calendar Year 2026
    private Integer plannedLeavesTotal;
    private Integer casualLeavesTotal;
    private Integer sickLeavesTotal;
    private Integer wfhAllowanceDays;
    private Integer compOffBalance;

    public CreateEmployeeRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public Integer getPlannedLeavesTotal() {
        return plannedLeavesTotal;
    }

    public void setPlannedLeavesTotal(Integer plannedLeavesTotal) {
        this.plannedLeavesTotal = plannedLeavesTotal;
    }

    public Integer getCasualLeavesTotal() {
        return casualLeavesTotal;
    }

    public void setCasualLeavesTotal(Integer casualLeavesTotal) {
        this.casualLeavesTotal = casualLeavesTotal;
    }

    public Integer getSickLeavesTotal() {
        return sickLeavesTotal;
    }

    public void setSickLeavesTotal(Integer sickLeavesTotal) {
        this.sickLeavesTotal = sickLeavesTotal;
    }

    public Integer getWfhAllowanceDays() {
        return wfhAllowanceDays;
    }

    public void setWfhAllowanceDays(Integer wfhAllowanceDays) {
        this.wfhAllowanceDays = wfhAllowanceDays;
    }

    public Integer getCompOffBalance() {
        return compOffBalance;
    }

    public void setCompOffBalance(Integer compOffBalance) {
        this.compOffBalance = compOffBalance;
    }
}
