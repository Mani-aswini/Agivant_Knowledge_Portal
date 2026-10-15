package com.example.knowledgeportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * EmployeeLeaveRecord Entity
 * 
 * Stores comprehensive leave balances and work allowances for an employee:
 * - Planned / Annual Leaves
 * - Casual Leaves
 * - Sick Leaves
 * - Work From Home (WFH)
 * - Comp Off
 * - Approved & Pending Counts
 */
@Entity
@Table(name = "employee_leaves")
public class EmployeeLeaveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "calendar_year", nullable = false)
    private int year = 2026;

    // Planned / Annual Leaves
    @Column(name = "planned_leaves_total", nullable = false)
    private int plannedLeavesTotal = 25;

    @Column(name = "planned_leaves_taken", nullable = false)
    private int plannedLeavesTaken = 0;

    @Column(name = "planned_leaves_remaining", nullable = false)
    private int plannedLeavesRemaining = 25;

    // Casual Leaves
    @Column(name = "casual_leaves_total", nullable = false)
    private int casualLeavesTotal = 6;

    @Column(name = "casual_leaves_taken", nullable = false)
    private int casualLeavesTaken = 0;

    @Column(name = "casual_leaves_remaining", nullable = false)
    private int casualLeavesRemaining = 6;

    // Sick Leaves
    @Column(name = "sick_leaves_total", nullable = false)
    private int sickLeavesTotal = 10;

    @Column(name = "sick_leaves_taken", nullable = false)
    private int sickLeavesTaken = 0;

    @Column(name = "sick_leaves_remaining", nullable = false)
    private int sickLeavesRemaining = 10;

    // Work From Home (WFH)
    @Column(name = "wfh_allowance_days", nullable = false)
    private int wfhAllowanceDays = 24;

    @Column(name = "wfh_used_days", nullable = false)
    private int wfhUsedDays = 0;

    @Column(name = "wfh_remaining_days", nullable = false)
    private int wfhRemainingDays = 24;

    // Comp Off
    @Column(name = "comp_off_earned", nullable = false)
    private int compOffEarned = 0;

    @Column(name = "comp_off_availed", nullable = false)
    private int compOffAvailed = 0;

    @Column(name = "comp_off_balance", nullable = false)
    private int compOffBalance = 0;

    // Counts
    @Column(name = "approved_leaves_count", nullable = false)
    private int approvedLeavesCount = 0;

    @Column(name = "pending_leaves_count", nullable = false)
    private int pendingLeavesCount = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public EmployeeLeaveRecord() {
    }

    public EmployeeLeaveRecord(String username, int year,
                               int plannedTotal, int plannedTaken,
                               int casualTotal, int casualTaken,
                               int sickTotal, int sickTaken,
                               int wfhAllowance, int wfhUsed,
                               int compOffEarned, int compOffAvailed,
                               int approvedCount, int pendingCount) {
        this.username = username;
        this.year = year;
        this.plannedLeavesTotal = plannedTotal;
        this.plannedLeavesTaken = plannedTaken;
        this.plannedLeavesRemaining = Math.max(0, plannedTotal - plannedTaken);

        this.casualLeavesTotal = casualTotal;
        this.casualLeavesTaken = casualTaken;
        this.casualLeavesRemaining = Math.max(0, casualTotal - casualTaken);

        this.sickLeavesTotal = sickTotal;
        this.sickLeavesTaken = sickTaken;
        this.sickLeavesRemaining = Math.max(0, sickTotal - sickTaken);

        this.wfhAllowanceDays = wfhAllowance;
        this.wfhUsedDays = wfhUsed;
        this.wfhRemainingDays = Math.max(0, wfhAllowance - wfhUsed);

        this.compOffEarned = compOffEarned;
        this.compOffAvailed = compOffAvailed;
        this.compOffBalance = Math.max(0, compOffEarned - compOffAvailed);

        this.approvedLeavesCount = approvedCount;
        this.pendingLeavesCount = pendingCount;
        this.lastUpdated = LocalDateTime.now();
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getPlannedLeavesTotal() {
        return plannedLeavesTotal;
    }

    public void setPlannedLeavesTotal(int plannedLeavesTotal) {
        this.plannedLeavesTotal = plannedLeavesTotal;
        this.plannedLeavesRemaining = Math.max(0, this.plannedLeavesTotal - this.plannedLeavesTaken);
    }

    public int getPlannedLeavesTaken() {
        return plannedLeavesTaken;
    }

    public void setPlannedLeavesTaken(int plannedLeavesTaken) {
        this.plannedLeavesTaken = plannedLeavesTaken;
        this.plannedLeavesRemaining = Math.max(0, this.plannedLeavesTotal - this.plannedLeavesTaken);
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
        this.casualLeavesRemaining = Math.max(0, this.casualLeavesTotal - this.casualLeavesTaken);
    }

    public int getCasualLeavesTaken() {
        return casualLeavesTaken;
    }

    public void setCasualLeavesTaken(int casualLeavesTaken) {
        this.casualLeavesTaken = casualLeavesTaken;
        this.casualLeavesRemaining = Math.max(0, this.casualLeavesTotal - this.casualLeavesTaken);
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
        this.sickLeavesRemaining = Math.max(0, this.sickLeavesTotal - this.sickLeavesTaken);
    }

    public int getSickLeavesTaken() {
        return sickLeavesTaken;
    }

    public void setSickLeavesTaken(int sickLeavesTaken) {
        this.sickLeavesTaken = sickLeavesTaken;
        this.sickLeavesRemaining = Math.max(0, this.sickLeavesTotal - this.sickLeavesTaken);
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
        this.wfhRemainingDays = Math.max(0, this.wfhAllowanceDays - this.wfhUsedDays);
    }

    public int getWfhUsedDays() {
        return wfhUsedDays;
    }

    public void setWfhUsedDays(int wfhUsedDays) {
        this.wfhUsedDays = wfhUsedDays;
        this.wfhRemainingDays = Math.max(0, this.wfhAllowanceDays - this.wfhUsedDays);
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
        this.compOffBalance = Math.max(0, this.compOffEarned - this.compOffAvailed);
    }

    public int getCompOffAvailed() {
        return compOffAvailed;
    }

    public void setCompOffAvailed(int compOffAvailed) {
        this.compOffAvailed = compOffAvailed;
        this.compOffBalance = Math.max(0, this.compOffEarned - this.compOffAvailed);
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
