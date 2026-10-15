package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.EmployeeLeaveRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Detailed employee response combining company profile and complete leave breakdown.
 */
public class EmployeeDetailResponse {

    // Profile Details
    private Long id;
    private String username;
    private String employeeCode;
    private String fullName;
    private String email;
    private String department;
    private String designation;
    private LocalDate dateOfJoining;
    private String location;
    private String reportingManager;
    private AccessLevel clearanceLevel;
    private String status;

    // Leave Balances
    private int plannedLeavesTotal;
    private int plannedLeavesTaken;
    private int plannedLeavesRemaining;

    private int casualLeavesTotal;
    private int casualLeavesTaken;
    private int casualLeavesRemaining;

    private int sickLeavesTotal;
    private int sickLeavesTaken;
    private int sickLeavesRemaining;

    private int wfhAllowanceDays;
    private int wfhUsedDays;
    private int wfhRemainingDays;

    private int compOffEarned;
    private int compOffAvailed;
    private int compOffBalance;

    private int approvedLeavesCount;
    private int pendingLeavesCount;

    // Applications history
    private List<EmployeeLeaveRequest> recentLeaveRequests;

    public EmployeeDetailResponse() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
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

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
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

    public AccessLevel getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(AccessLevel clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPlannedLeavesTotal() {
        return plannedLeavesTotal;
    }

    public void setPlannedLeavesTotal(int plannedLeavesTotal) {
        this.plannedLeavesTotal = plannedLeavesTotal;
    }

    public int getPlannedLeavesTaken() {
        return plannedLeavesTaken;
    }

    public void setPlannedLeavesTaken(int plannedLeavesTaken) {
        this.plannedLeavesTaken = plannedLeavesTaken;
    }

    public int getPlannedLeavesRemaining() {
        return plannedLeavesRemaining;
    }

    public void setPlannedLeavesRemaining(int plannedLeavesRemaining) {
        this.plannedLeavesRemaining = plannedLeavesRemaining;
    }

    public int getCasualLeavesTotal() {
        return casualLeavesTotal;
    }

    public void setCasualLeavesTotal(int casualLeavesTotal) {
        this.casualLeavesTotal = casualLeavesTotal;
    }

    public int getCasualLeavesTaken() {
        return casualLeavesTaken;
    }

    public void setCasualLeavesTaken(int casualLeavesTaken) {
        this.casualLeavesTaken = casualLeavesTaken;
    }

    public int getCasualLeavesRemaining() {
        return casualLeavesRemaining;
    }

    public void setCasualLeavesRemaining(int casualLeavesRemaining) {
        this.casualLeavesRemaining = casualLeavesRemaining;
    }

    public int getSickLeavesTotal() {
        return sickLeavesTotal;
    }

    public void setSickLeavesTotal(int sickLeavesTotal) {
        this.sickLeavesTotal = sickLeavesTotal;
    }

    public int getSickLeavesTaken() {
        return sickLeavesTaken;
    }

    public void setSickLeavesTaken(int sickLeavesTaken) {
        this.sickLeavesTaken = sickLeavesTaken;
    }

    public int getSickLeavesRemaining() {
        return sickLeavesRemaining;
    }

    public void setSickLeavesRemaining(int sickLeavesRemaining) {
        this.sickLeavesRemaining = sickLeavesRemaining;
    }

    public int getWfhAllowanceDays() {
        return wfhAllowanceDays;
    }

    public void setWfhAllowanceDays(int wfhAllowanceDays) {
        this.wfhAllowanceDays = wfhAllowanceDays;
    }

    public int getWfhUsedDays() {
        return wfhUsedDays;
    }

    public void setWfhUsedDays(int wfhUsedDays) {
        this.wfhUsedDays = wfhUsedDays;
    }

    public int getWfhRemainingDays() {
        return wfhRemainingDays;
    }

    public void setWfhRemainingDays(int wfhRemainingDays) {
        this.wfhRemainingDays = wfhRemainingDays;
    }

    public int getCompOffEarned() {
        return compOffEarned;
    }

    public void setCompOffEarned(int compOffEarned) {
        this.compOffEarned = compOffEarned;
    }

    public int getCompOffAvailed() {
        return compOffAvailed;
    }

    public void setCompOffAvailed(int compOffAvailed) {
        this.compOffAvailed = compOffAvailed;
    }

    public int getCompOffBalance() {
        return compOffBalance;
    }

    public void setCompOffBalance(int compOffBalance) {
        this.compOffBalance = compOffBalance;
    }

    public int getApprovedLeavesCount() {
        return approvedLeavesCount;
    }

    public void setApprovedLeavesCount(int approvedLeavesCount) {
        this.approvedLeavesCount = approvedLeavesCount;
    }

    public int getPendingLeavesCount() {
        return pendingLeavesCount;
    }

    public void setPendingLeavesCount(int pendingLeavesCount) {
        this.pendingLeavesCount = pendingLeavesCount;
    }

    public List<EmployeeLeaveRequest> getRecentLeaveRequests() {
        return recentLeaveRequests;
    }

    public void setRecentLeaveRequests(List<EmployeeLeaveRequest> recentLeaveRequests) {
        this.recentLeaveRequests = recentLeaveRequests;
    }
}
