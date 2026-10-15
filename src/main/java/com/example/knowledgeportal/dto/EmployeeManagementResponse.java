package com.example.knowledgeportal.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated response for the Admin Employees Information & Leave Management section.
 */
public class EmployeeManagementResponse {

    private long totalEmployees;
    private long totalPendingRequests;
    private long totalApprovedLeaves;
    private long totalWfhDaysUsed;
    private Map<String, Long> departmentCounts;
    private List<EmployeeDetailResponse> employees;

    public EmployeeManagementResponse() {
    }

    public EmployeeManagementResponse(long totalEmployees, long totalPendingRequests,
                                      long totalApprovedLeaves, long totalWfhDaysUsed,
                                      Map<String, Long> departmentCounts,
                                      List<EmployeeDetailResponse> employees) {
        this.totalEmployees = totalEmployees;
        this.totalPendingRequests = totalPendingRequests;
        this.totalApprovedLeaves = totalApprovedLeaves;
        this.totalWfhDaysUsed = totalWfhDaysUsed;
        this.departmentCounts = departmentCounts;
        this.employees = employees;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getTotalPendingRequests() {
        return totalPendingRequests;
    }

    public void setTotalPendingRequests(long totalPendingRequests) {
        this.totalPendingRequests = totalPendingRequests;
    }

    public long getTotalApprovedLeaves() {
        return totalApprovedLeaves;
    }

    public void setTotalApprovedLeaves(long totalApprovedLeaves) {
        this.totalApprovedLeaves = totalApprovedLeaves;
    }

    public long getTotalWfhDaysUsed() {
        return totalWfhDaysUsed;
    }

    public void setTotalWfhDaysUsed(long totalWfhDaysUsed) {
        this.totalWfhDaysUsed = totalWfhDaysUsed;
    }

    public Map<String, Long> getDepartmentCounts() {
        return departmentCounts;
    }

    public void setDepartmentCounts(Map<String, Long> departmentCounts) {
        this.departmentCounts = departmentCounts;
    }

    public List<EmployeeDetailResponse> getEmployees() {
        return employees;
    }

    public void setEmployees(List<EmployeeDetailResponse> employees) {
        this.employees = employees;
    }
}
