package com.example.knowledgeportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new company employee.
 * Admin or HR specifies login credentials (username, password, role)
 * and corporate profile details (department, designation, initial leave allocation).
 */
public class CreateEmployeeRequest {

    @NotBlank(message = "Username is required for employee login")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-z][a-z0-9._]{2,29}$",
            message = "Username must start with a lowercase letter, contain only lowercase letters, digits, dots or underscores (3-30 characters), and cannot be numbers only")
    private String username;

    @NotBlank(message = "Password is required for employee login")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,100}$",
            message = "Password must be at least 6 characters long and contain a mix of letters and numbers/symbols")
    private String password;

    @NotBlank(message = "Full Name is required")
    @Size(min = 2, max = 100, message = "Full Name must be between 2 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-zA-Z].*[a-zA-Z])[a-zA-Z\\s.'-]+$",
            message = "Full Name must contain only alphabetic letters, spaces, and standard name characters (cannot be numbers or numeric only)")
    private String fullName;

    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Designation/Job Title is required")
    private String designation;

    private String employeeCode;

    private String role; // ROLE_EMPLOYEE, ROLE_HR, ROLE_ADMIN

    private String location;

    @Pattern(regexp = "^([a-zA-Z\\s.'-]+)?$",
            message = "Reporting Manager must contain only letters and valid name characters (cannot contain numbers)")
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
