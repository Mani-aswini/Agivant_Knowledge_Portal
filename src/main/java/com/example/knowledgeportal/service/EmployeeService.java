package com.example.knowledgeportal.service;

import com.example.knowledgeportal.dto.EmployeeDetailResponse;
import com.example.knowledgeportal.dto.EmployeeManagementResponse;
import com.example.knowledgeportal.entity.EmployeeLeaveRecord;
import com.example.knowledgeportal.entity.EmployeeLeaveRequest;
import com.example.knowledgeportal.entity.EmployeeProfile;
import com.example.knowledgeportal.exception.ResourceNotFoundException;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import com.example.knowledgeportal.repository.EmployeeLeaveRecordRepository;
import com.example.knowledgeportal.repository.EmployeeLeaveRequestRepository;
import com.example.knowledgeportal.repository.EmployeeProfileRepository;
import com.example.knowledgeportal.security.PermissionService;
import org.springframework.security.core.Authentication;
import com.example.knowledgeportal.dto.CreateEmployeeRequest;
import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.UserAccount;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeProfileRepository profileRepository;
    private final EmployeeLeaveRecordRepository leaveRepository;
    private final EmployeeLeaveRequestRepository requestRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public EmployeeService(EmployeeProfileRepository profileRepository,
                           EmployeeLeaveRecordRepository leaveRepository,
                           EmployeeLeaveRequestRepository requestRepository,
                           UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           PermissionService permissionService) {
        this.profileRepository = profileRepository;
        this.leaveRepository = leaveRepository;
        this.requestRepository = requestRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    /**
     * Creates a new company employee record and corresponding UserAccount credentials.
     * Allows the created employee to immediately log into the system with their username & password.
     * Restricted to Admin & HR.
     */
    @Transactional
    public EmployeeDetailResponse createEmployee(CreateEmployeeRequest req, Authentication auth) {
        permissionService.validateAdminOrHr(auth);

        String username = req.getUsername().trim().toLowerCase();
        if (userAccountRepository.existsByUsernameIgnoreCase(username) || profileRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalStateException("Username '" + username + "' is already registered. Please choose another username.");
        }

        // Generate or validate employee code
        String employeeCode = req.getEmployeeCode() != null && !req.getEmployeeCode().isBlank()
                ? req.getEmployeeCode().trim().toUpperCase()
                : generateNextEmployeeCode();

        if (profileRepository.findByEmployeeCode(employeeCode).isPresent()) {
            throw new IllegalStateException("Employee code '" + employeeCode + "' is already assigned to another employee.");
        }

        // Determine Role and Clearance
        UserRole userRole = UserRole.ROLE_EMPLOYEE;
        AccessLevel clearance = AccessLevel.EMPLOYEE;
        if (req.getRole() != null) {
            String r = req.getRole().trim().toUpperCase();
            if (r.contains("ADMIN")) {
                userRole = UserRole.ROLE_ADMIN;
                clearance = AccessLevel.ADMIN;
            } else if (r.contains("HR")) {
                userRole = UserRole.ROLE_HR;
                clearance = AccessLevel.HR;
            }
        }

        // Email
        String email = req.getEmail() != null && !req.getEmail().isBlank()
                ? req.getEmail().trim().toLowerCase()
                : username + "@company.internal";

        String location = req.getLocation() != null && !req.getLocation().isBlank()
                ? req.getLocation().trim()
                : "Main Office / Hybrid";

        String reportingManager = req.getReportingManager() != null && !req.getReportingManager().isBlank()
                ? req.getReportingManager().trim()
                : "Ashwin Mani";

        // 1. Create and persist UserAccount for authentication
        String encodedPassword = passwordEncoder.encode(req.getPassword().trim());
        UserAccount userAccount = new UserAccount(username, encodedPassword, req.getFullName().trim(), userRole);
        userAccountRepository.save(userAccount);

        // 2. Create and persist EmployeeProfile
        EmployeeProfile profile = new EmployeeProfile(
                username,
                employeeCode,
                req.getFullName().trim(),
                email,
                req.getDepartment().trim(),
                req.getDesignation().trim(),
                LocalDate.now(),
                location,
                reportingManager,
                clearance
        );
        EmployeeProfile savedProfile = profileRepository.save(profile);

        // 3. Create and persist initial EmployeeLeaveRecord
        int plannedTotal = req.getPlannedLeavesTotal() != null ? Math.max(0, req.getPlannedLeavesTotal()) : 25;
        int casualTotal = req.getCasualLeavesTotal() != null ? Math.max(0, req.getCasualLeavesTotal()) : 6;
        int sickTotal = req.getSickLeavesTotal() != null ? Math.max(0, req.getSickLeavesTotal()) : 10;
        int wfhTotal = req.getWfhAllowanceDays() != null ? Math.max(0, req.getWfhAllowanceDays()) : 24;
        int compOffBal = req.getCompOffBalance() != null ? Math.max(0, req.getCompOffBalance()) : 0;

        EmployeeLeaveRecord leave = new EmployeeLeaveRecord(
                username,
                2026,
                plannedTotal, 0,
                casualTotal, 0,
                sickTotal, 0,
                wfhTotal, 0,
                compOffBal, 0,
                0, 0
        );
        EmployeeLeaveRecord savedLeave = leaveRepository.save(leave);

        return mapToDetailResponse(savedProfile, savedLeave, Collections.emptyList());
    }

    private String generateNextEmployeeCode() {
        long count = profileRepository.count();
        long nextNum = 1001 + count;
        String code = "EMP-" + nextNum;
        while (profileRepository.findByEmployeeCode(code).isPresent()) {
            nextNum++;
            code = "EMP-" + nextNum;
        }
        return code;
    }

    /**
     * Retrieves aggregated employee directory and leave information.
     * Restricted to ROLE_ADMIN and ROLE_HR.
     */
    @Transactional(readOnly = true)
    public EmployeeManagementResponse getEmployeesOverview(String department, String search, Authentication auth) {
        permissionService.validateAdminOrHr(auth);

        List<EmployeeProfile> allProfiles = profileRepository.findAllByOrderByFullNameAsc();

        // Calculate Department counts
        Map<String, Long> departmentCounts = allProfiles.stream()
                .collect(Collectors.groupingBy(EmployeeProfile::getDepartment, LinkedHashMap::new, Collectors.counting()));

        // Filter profiles
        List<EmployeeProfile> filteredProfiles = allProfiles.stream()
                .filter(p -> {
                    if (department != null && !department.isBlank() && !department.equalsIgnoreCase("ALL")) {
                        if (!p.getDepartment().equalsIgnoreCase(department.trim())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.trim().toLowerCase();
                        boolean matchName = p.getFullName().toLowerCase().contains(s);
                        boolean matchCode = p.getEmployeeCode().toLowerCase().contains(s);
                        boolean matchDept = p.getDepartment().toLowerCase().contains(s);
                        boolean matchDesig = p.getDesignation().toLowerCase().contains(s);
                        boolean matchUser = p.getUsername().toLowerCase().contains(s);
                        return matchName || matchCode || matchDept || matchDesig || matchUser;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<EmployeeDetailResponse> items = new ArrayList<>();
        long totalPending = 0;
        long totalApproved = 0;
        long totalWfhUsed = 0;

        for (EmployeeProfile profile : filteredProfiles) {
            EmployeeLeaveRecord leave = leaveRepository.findByUsername(profile.getUsername())
                    .orElseGet(() -> new EmployeeLeaveRecord(profile.getUsername(), 2026, 25, 0, 6, 0, 10, 0, 24, 0, 0, 0, 0, 0));

            List<EmployeeLeaveRequest> requests = requestRepository.findByUsernameOrderByStartDateDesc(profile.getUsername());

            EmployeeDetailResponse detail = mapToDetailResponse(profile, leave, requests);
            items.add(detail);

            totalPending += leave.getPendingLeavesCount();
            totalApproved += leave.getApprovedLeavesCount();
            totalWfhUsed += leave.getWfhUsedDays();
        }

        return new EmployeeManagementResponse(
                allProfiles.size(),
                totalPending,
                totalApproved,
                totalWfhUsed,
                departmentCounts,
                items
        );
    }

    /**
     * Retrieves specific employee details.
     * Allowed for Admin, HR, or the specific employee themselves.
     */
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetails(String username, Authentication auth) {
        String currentUsername = permissionService.getCurrentUsername(auth);
        boolean isAdminOrHr = permissionService.isUserAdminOrHr(auth);

        if (!isAdminOrHr && !currentUsername.equalsIgnoreCase(username)) {
            throw new UnauthorizedAccessException("You are not authorized to view employee details for: " + username);
        }

        EmployeeProfile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for: " + username));

        EmployeeLeaveRecord leave = leaveRepository.findByUsername(username)
                .orElseGet(() -> new EmployeeLeaveRecord(profile.getUsername(), 2026, 25, 0, 6, 0, 10, 0, 24, 0, 0, 0, 0, 0));

        List<EmployeeLeaveRequest> requests = requestRepository.findByUsernameOrderByStartDateDesc(username);

        return mapToDetailResponse(profile, leave, requests);
    }

    /**
     * Retrieves currently logged in employee's details.
     */
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getCurrentEmployeeDetails(Authentication auth) {
        String currentUsername = permissionService.getCurrentUsername(auth);
        return getEmployeeDetails(currentUsername, auth);
    }

    /**
     * Formats employee profile and leave records as verified reference context for AI Chatbot answering.
     */
    @Transactional(readOnly = true)
    public Optional<String> getEmployeeLeaveContextForAi(String username) {
        return getEmployeeContextForAi(username);
    }

    /**
     * Formats complete employee profile, department, location, and leave records for AI Chatbot answering.
     */
    @Transactional(readOnly = true)
    public Optional<String> getEmployeeContextForAi(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        Optional<EmployeeProfile> profileOpt = profileRepository.findByUsernameIgnoreCase(username.trim());
        if (profileOpt.isEmpty()) {
            profileOpt = profileRepository.findByEmployeeCode(username.trim());
        }

        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }

        EmployeeProfile p = profileOpt.get();
        EmployeeLeaveRecord l = leaveRepository.findByUsernameIgnoreCase(p.getUsername())
                .orElseGet(() -> new EmployeeLeaveRecord(p.getUsername(), 2026, 25, 0, 6, 0, 10, 0, 24, 0, 0, 0, 0, 0));

        List<EmployeeLeaveRequest> requests = requestRepository.findByUsernameOrderByStartDateDesc(p.getUsername());

        StringBuilder sb = new StringBuilder();
        sb.append("Verified Enterprise Employee Profile & HR Records:\n");
        sb.append("• Full Name: ").append(p.getFullName()).append("\n");
        sb.append("• Employee Code: ").append(p.getEmployeeCode()).append("\n");
        sb.append("• Username / Account: ").append(p.getUsername()).append("\n");
        sb.append("• Corporate Email: ").append(p.getEmail()).append("\n");
        sb.append("• Department: ").append(p.getDepartment()).append("\n");
        sb.append("• Designation: ").append(p.getDesignation()).append("\n");
        sb.append("• Primary Work Location: ").append(p.getLocation()).append("\n");
        sb.append("• Reporting Manager: ").append(p.getReportingManager() != null ? p.getReportingManager() : "Executive Management").append("\n");
        sb.append("• Date of Joining: ").append(p.getDateOfJoining()).append("\n");
        sb.append("• Security Clearance Level: ").append(p.getClearanceLevel()).append("\n");
        sb.append("• Employment Status: ").append(p.getStatus()).append("\n\n");

        sb.append("Leave Balances & Attendance Summary (Calendar Year 2026):\n");
        sb.append("• Planned / Annual Leaves: ").append(l.getPlannedLeavesRemaining())
                .append(" days remaining (Total: ").append(l.getPlannedLeavesTotal())
                .append(", Taken: ").append(l.getPlannedLeavesTaken()).append(")\n");
        sb.append("• Casual Leaves: ").append(l.getCasualLeavesRemaining())
                .append(" days remaining (Total: ").append(l.getCasualLeavesTotal())
                .append(", Taken: ").append(l.getCasualLeavesTaken()).append(")\n");
        sb.append("• Sick Leaves: ").append(l.getSickLeavesRemaining())
                .append(" days remaining (Total: ").append(l.getSickLeavesTotal())
                .append(", Taken: ").append(l.getSickLeavesTaken()).append(")\n");
        sb.append("• Work From Home (WFH): ").append(l.getWfhRemainingDays())
                .append(" days remaining (Allowance: ").append(l.getWfhAllowanceDays())
                .append(", Used: ").append(l.getWfhUsedDays()).append(")\n");
        sb.append("• Comp Off: ").append(l.getCompOffBalance())
                .append(" days balance (Earned: ").append(l.getCompOffEarned())
                .append(", Availed: ").append(l.getCompOffAvailed()).append(")\n");
        sb.append("• Total Approved Leaves Taken: ").append(l.getApprovedLeavesCount()).append(" days\n");
        sb.append("• Pending Leave Requests: ").append(l.getPendingLeavesCount()).append("\n");

        if (!requests.isEmpty()) {
            sb.append("\nRecent Leave Applications:\n");
            for (EmployeeLeaveRequest req : requests) {
                sb.append("- [").append(req.getStatus()).append("] ").append(req.getLeaveType())
                        .append(" from ").append(req.getStartDate()).append(" to ").append(req.getEndDate())
                        .append(" (").append(req.getDaysCount()).append(" day(s)) - Reason: ").append(req.getReason()).append("\n");
            }
        }

        return Optional.of(sb.toString());
    }

    /**
     * Searches employee data for AI Assistant answering, supporting both personal queries and directory lookups.
     */
    @Transactional(readOnly = true)
    public Optional<String> searchEmployeeContextForAi(String query, String callerUsername, boolean isAdminOrHr) {
        if (query == null) query = "";
        String lowerQuery = query.toLowerCase().trim();

        List<EmployeeProfile> allProfiles = profileRepository.findAllByOrderByFullNameAsc();

        // 1. Check if user explicitly asked about themselves ("me", "my details", "about me", "who am i", etc.)
        if (isQueryAboutSelf(lowerQuery)) {
            return getEmployeeContextForAi(callerUsername);
        }

        // 2. Check if a specific colleague or employee code was mentioned
        EmployeeProfile targetColleague = findSpecificTargetEmployee(lowerQuery, allProfiles);
        if (targetColleague != null) {
            if (targetColleague.getUsername().equalsIgnoreCase(callerUsername)) {
                return getEmployeeContextForAi(callerUsername);
            }
            return formatSingleProfileForAi(targetColleague, isAdminOrHr);
        }

        // 3. General employee query ("employee details", "all employees", "details about all employees", directory roster)
        // In Admin/HR Portal: Gives the whole employee details for all employees
        // In Employee Portal: Gives only Name, Role, Department, and Location (NO leaves or personal data)
        return formatAllEmployeesDirectoryForAi(allProfiles, lowerQuery, isAdminOrHr);
    }

    private boolean isQueryAboutSelf(String lower) {
        if (lower.contains("about me") || lower.contains("about myself") || lower.contains("details about me") ||
                lower.contains("data about me") || lower.contains("tell me about me") || lower.contains("data of mine")) {
            return true;
        }
        if (lower.contains("my details") || lower.contains("my data") || lower.contains("my profile") ||
                lower.contains("my record") || lower.contains("my info") || lower.contains("my information")) {
            return true;
        }
        if (lower.contains("my name") || lower.contains("who am i") || lower.contains("where do i work") ||
                lower.contains("my work location") || lower.contains("my location") || lower.contains("my office") ||
                lower.contains("my department") || lower.contains("my designation") || lower.contains("my role") ||
                lower.contains("my manager") || lower.contains("who is my manager") || lower.contains("who do i report to") ||
                lower.contains("my employee code") || lower.contains("my employee id") || lower.contains("my emp code") ||
                lower.contains("when did i join") || lower.contains("my joining date")) {
            return true;
        }
        if (lower.contains("my leave") || lower.contains("my leaves") || lower.contains("my wfh") ||
                lower.contains("did i have") || lower.contains("do i have") || lower.contains("how many leaves did i have") ||
                lower.contains("my comp off") || lower.contains("my sick leave") || lower.contains("my casual leave")) {
            return true;
        }
        if (lower.contains("logined person") || lower.contains("logged in person") ||
                lower.contains("logined user") || lower.contains("logged in user") ||
                lower.contains("current user") || lower.contains("current employee")) {
            return true;
        }
        if (lower.startsWith("give me my") || lower.startsWith("show my") ||
                lower.startsWith("tell me my") || lower.startsWith("what is my") || lower.startsWith("what are my")) {
            return true;
        }
        return false;
    }

    private EmployeeProfile findSpecificTargetEmployee(String lowerQuery, List<EmployeeProfile> allProfiles) {
        // 1. Exact employee code match (e.g. "EMP-1001", "EMP-1000")
        for (EmployeeProfile p : allProfiles) {
            if (lowerQuery.contains(p.getEmployeeCode().toLowerCase())) {
                return p;
            }
        }

        // 2. Exact username or full name match
        for (EmployeeProfile p : allProfiles) {
            String fName = p.getFullName().toLowerCase();
            String uName = p.getUsername().toLowerCase();
            if (lowerQuery.contains(uName) || (fName.length() >= 5 && lowerQuery.contains(fName))) {
                return p;
            }
        }

        // 3. First name / unique token match, skipping generic corporate stop words
        Set<String> stopWords = Set.of(
                "employee", "employees", "employe", "employess", "user", "users",
                "manager", "managers", "administrator", "administrators", "admin",
                "staff", "person", "people", "details", "detail", "data", "profile",
                "give", "show", "tell", "what", "which", "about", "whole", "entire", "all"
        );
        for (EmployeeProfile p : allProfiles) {
            String[] nameParts = p.getFullName().toLowerCase().split("\\s+");
            for (String part : nameParts) {
                if (part.length() >= 4 && !stopWords.contains(part) && lowerQuery.contains(part)) {
                    return p;
                }
            }
        }
        return null;
    }

    private Optional<String> formatSingleProfileForAi(EmployeeProfile p, boolean isAdminOrHr) {
        StringBuilder sb = new StringBuilder();

        if (isAdminOrHr) {
            // ADMIN / HR VIEW: Complete whole details of the specific employee
            sb.append("Verified Enterprise Employee Profile (Administrator / HR View):\n\n");
            sb.append("• Full Name: ").append(p.getFullName()).append("\n");
            sb.append("• Employee Code: ").append(p.getEmployeeCode()).append("\n");
            sb.append("• Username / Account: ").append(p.getUsername()).append("\n");
            sb.append("• Corporate Email: ").append(p.getEmail()).append("\n");
            sb.append("• Department: ").append(p.getDepartment()).append("\n");
            sb.append("• Designation / Role: ").append(p.getDesignation()).append("\n");
            sb.append("• Primary Work Location: ").append(p.getLocation()).append("\n");
            sb.append("• Reporting Manager: ").append(p.getReportingManager() != null ? p.getReportingManager() : "Executive Management").append("\n");
            sb.append("• Date of Joining: ").append(p.getDateOfJoining()).append("\n");
            sb.append("• Security Clearance Level: ").append(p.getClearanceLevel()).append("\n");
            sb.append("• Employment Status: ").append(p.getStatus()).append("\n\n");

            EmployeeLeaveRecord l = leaveRepository.findByUsernameIgnoreCase(p.getUsername())
                    .orElseGet(() -> new EmployeeLeaveRecord(p.getUsername(), 2026, 25, 0, 6, 0, 10, 0, 24, 0, 0, 0, 0, 0));

            sb.append("Leave Balances & Attendance Summary (Calendar Year 2026):\n");
            sb.append("• Planned / Annual Leaves: ").append(l.getPlannedLeavesRemaining())
                    .append(" days remaining (Total: ").append(l.getPlannedLeavesTotal())
                    .append(", Taken: ").append(l.getPlannedLeavesTaken()).append(")\n");
            sb.append("• Casual Leaves: ").append(l.getCasualLeavesRemaining())
                    .append(" days remaining (Total: ").append(l.getCasualLeavesTotal())
                    .append(", Taken: ").append(l.getCasualLeavesTaken()).append(")\n");
            sb.append("• Sick Leaves: ").append(l.getSickLeavesRemaining())
                    .append(" days remaining (Total: ").append(l.getSickLeavesTotal())
                    .append(", Taken: ").append(l.getSickLeavesTaken()).append(")\n");
            sb.append("• Work From Home (WFH): ").append(l.getWfhRemainingDays())
                    .append(" days remaining (Allowance: ").append(l.getWfhAllowanceDays())
                    .append(", Used: ").append(l.getWfhUsedDays()).append(")\n");
            sb.append("• Comp Off: ").append(l.getCompOffBalance())
                    .append(" days balance (Earned: ").append(l.getCompOffEarned())
                    .append(", Availed: ").append(l.getCompOffAvailed()).append(")\n");
            sb.append("• Total Approved Leaves Taken: ").append(l.getApprovedLeavesCount()).append(" days\n");
            sb.append("• Pending Leave Requests: ").append(l.getPendingLeavesCount()).append("\n");

            List<EmployeeLeaveRequest> requests = requestRepository.findByUsernameOrderByStartDateDesc(p.getUsername());
            if (!requests.isEmpty()) {
                sb.append("\nRecent Leave Applications:\n");
                for (EmployeeLeaveRequest req : requests) {
                    sb.append("- [").append(req.getStatus()).append("] ").append(req.getLeaveType())
                            .append(" from ").append(req.getStartDate()).append(" to ").append(req.getEndDate())
                            .append(" (").append(req.getDaysCount()).append(" day(s)) - Reason: ").append(req.getReason()).append("\n");
                }
            }
        } else {
            // REGULAR EMPLOYEE VIEW: Only Name, Role, Department, and Location
            sb.append("Verified Enterprise Employee Directory Profile:\n\n");
            sb.append("• Full Name: ").append(p.getFullName()).append("\n");
            sb.append("• Role / Designation: ").append(p.getDesignation()).append("\n");
            sb.append("• Department: ").append(p.getDepartment()).append("\n");
            sb.append("• Work Location: ").append(p.getLocation()).append("\n");
            sb.append("• Employee Code: ").append(p.getEmployeeCode()).append("\n");
            sb.append("• Reporting Manager: ").append(p.getReportingManager() != null ? p.getReportingManager() : "N/A").append("\n");
            sb.append("• Corporate Email: ").append(p.getEmail()).append("\n\n");
            sb.append("*(Note: Colleague leave quotas, attendance records, and personal HR details are protected by Company Privacy Policy and restricted to HR and Portal Administrators).*");
        }

        return Optional.of(sb.toString());
    }

    private Optional<String> formatAllEmployeesDirectoryForAi(List<EmployeeProfile> allProfiles, String lowerQuery, boolean isAdminOrHr) {
        StringBuilder sb = new StringBuilder();

        if (isAdminOrHr) {
            // ADMIN / HR VIEW: Complete whole employee details for all employees
            sb.append("Enterprise Employee Directory — Complete Personnel & HR Records (Admin / HR View):\n\n");
            int idx = 1;
            for (EmployeeProfile p : allProfiles) {
                if (lowerQuery.contains("engineering") && !p.getDepartment().equalsIgnoreCase("Engineering")) continue;
                if (lowerQuery.contains("human resources") && !p.getDepartment().equalsIgnoreCase("Human Resources")) continue;
                if (lowerQuery.contains("administration") && !p.getDepartment().equalsIgnoreCase("Administration")) continue;

                sb.append("### ").append(idx++).append(". ").append(p.getFullName())
                        .append(" (").append(p.getEmployeeCode()).append(")\n");
                sb.append("• Role / Designation: ").append(p.getDesignation()).append("\n");
                sb.append("• Department: ").append(p.getDepartment()).append("\n");
                sb.append("• Primary Work Location: ").append(p.getLocation()).append("\n");
                sb.append("• Reporting Manager: ").append(p.getReportingManager() != null ? p.getReportingManager() : "Executive Management").append("\n");
                sb.append("• Corporate Email: ").append(p.getEmail()).append(" | Username: ").append(p.getUsername()).append("\n");
                sb.append("• Date of Joining: ").append(p.getDateOfJoining()).append(" | Status: ").append(p.getStatus()).append("\n");
                sb.append("• Security Clearance Level: ").append(p.getClearanceLevel()).append("\n");

                EmployeeLeaveRecord l = leaveRepository.findByUsernameIgnoreCase(p.getUsername())
                        .orElseGet(() -> new EmployeeLeaveRecord(p.getUsername(), 2026, 25, 0, 6, 0, 10, 0, 24, 0, 0, 0, 0, 0));

                sb.append("• Leave & Attendance Balances (2026):\n");
                sb.append("   - Planned Leaves Remaining: ").append(l.getPlannedLeavesRemaining()).append(" / ").append(l.getPlannedLeavesTotal())
                        .append(" (Taken: ").append(l.getPlannedLeavesTaken()).append(")\n");
                sb.append("   - Casual Leaves Remaining: ").append(l.getCasualLeavesRemaining()).append(" / ").append(l.getCasualLeavesTotal())
                        .append(" (Taken: ").append(l.getCasualLeavesTaken()).append(")\n");
                sb.append("   - Sick Leaves Remaining: ").append(l.getSickLeavesRemaining()).append(" / ").append(l.getSickLeavesTotal())
                        .append(" (Taken: ").append(l.getSickLeavesTaken()).append(")\n");
                sb.append("   - Work From Home (WFH): ").append(l.getWfhRemainingDays()).append(" / ").append(l.getWfhAllowanceDays())
                        .append(" (Used: ").append(l.getWfhUsedDays()).append(")\n");
                sb.append("   - Comp Off Balance: ").append(l.getCompOffBalance()).append(" days\n");
                sb.append("   - Approved Leaves: ").append(l.getApprovedLeavesCount()).append(" days | Pending Requests: ").append(l.getPendingLeavesCount()).append("\n\n");
            }
        } else {
            // REGULAR EMPLOYEE VIEW: Only Name, Role, Department, and Location
            sb.append("Enterprise Employee Directory Roster:\n");
            sb.append("Here is the authorized corporate directory (showing Name, Role, Department, and Location):\n\n");
            int idx = 1;
            for (EmployeeProfile p : allProfiles) {
                if (lowerQuery.contains("engineering") && !p.getDepartment().equalsIgnoreCase("Engineering")) continue;
                if (lowerQuery.contains("human resources") && !p.getDepartment().equalsIgnoreCase("Human Resources")) continue;
                if (lowerQuery.contains("administration") && !p.getDepartment().equalsIgnoreCase("Administration")) continue;

                sb.append(idx++).append(". **").append(p.getFullName()).append("** (").append(p.getEmployeeCode()).append(")\n");
                sb.append("   • Role / Designation: ").append(p.getDesignation()).append("\n");
                sb.append("   • Department: ").append(p.getDepartment()).append("\n");
                sb.append("   • Work Location: ").append(p.getLocation()).append("\n");
                sb.append("   • Corporate Email: ").append(p.getEmail()).append("\n\n");
            }
            sb.append("*(Note: Colleague leave quotas, attendance records, and confidential personal data are protected by Enterprise Privacy Policy and restricted to HR and Administrators).*");
        }

        return Optional.of(sb.toString().trim());
    }


    private EmployeeDetailResponse mapToDetailResponse(EmployeeProfile profile, EmployeeLeaveRecord leave, List<EmployeeLeaveRequest> requests) {
        EmployeeDetailResponse d = new EmployeeDetailResponse();
        d.setId(profile.getId());
        d.setUsername(profile.getUsername());
        d.setEmployeeCode(profile.getEmployeeCode());
        d.setFullName(profile.getFullName());
        d.setEmail(profile.getEmail());
        d.setDepartment(profile.getDepartment());
        d.setDesignation(profile.getDesignation());
        d.setDateOfJoining(profile.getDateOfJoining());
        d.setLocation(profile.getLocation());
        d.setReportingManager(profile.getReportingManager());
        d.setClearanceLevel(profile.getClearanceLevel());
        d.setStatus(profile.getStatus());

        d.setPlannedLeavesTotal(leave.getPlannedLeavesTotal());
        d.setPlannedLeavesTaken(leave.getPlannedLeavesTaken());
        d.setPlannedLeavesRemaining(leave.getPlannedLeavesRemaining());

        d.setCasualLeavesTotal(leave.getCasualLeavesTotal());
        d.setCasualLeavesTaken(leave.getCasualLeavesTaken());
        d.setCasualLeavesRemaining(leave.getCasualLeavesRemaining());

        d.setSickLeavesTotal(leave.getSickLeavesTotal());
        d.setSickLeavesTaken(leave.getSickLeavesTaken());
        d.setSickLeavesRemaining(leave.getSickLeavesRemaining());

        d.setWfhAllowanceDays(leave.getWfhAllowanceDays());
        d.setWfhUsedDays(leave.getWfhUsedDays());
        d.setWfhRemainingDays(leave.getWfhRemainingDays());

        d.setCompOffEarned(leave.getCompOffEarned());
        d.setCompOffAvailed(leave.getCompOffAvailed());
        d.setCompOffBalance(leave.getCompOffBalance());

        d.setApprovedLeavesCount(leave.getApprovedLeavesCount());
        d.setPendingLeavesCount(leave.getPendingLeavesCount());

        d.setRecentLeaveRequests(requests);

        return d;
    }
}
