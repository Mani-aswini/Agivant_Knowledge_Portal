package com.example.knowledgeportal.config;

import com.example.knowledgeportal.entity.*;
import com.example.knowledgeportal.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DataInitializer ensures default test accounts, approved policy documents,
 * and chunks are seeded if the database is initially empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeFeedbackRepository feedbackRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final EmployeeProfileRepository profileRepository;
    private final EmployeeLeaveRecordRepository leaveRecordRepository;
    private final EmployeeLeaveRequestRepository leaveRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userRepository,
                           KnowledgeDocumentRepository documentRepository,
                           KnowledgeChunkRepository chunkRepository,
                           KnowledgeFeedbackRepository feedbackRepository,
                           SearchHistoryRepository searchHistoryRepository,
                           EmployeeProfileRepository profileRepository,
                           EmployeeLeaveRecordRepository leaveRecordRepository,
                           EmployeeLeaveRequestRepository leaveRequestRepository,
                           ChatMessageRepository chatMessageRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.feedbackRepository = feedbackRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.profileRepository = profileRepository;
        this.leaveRecordRepository = leaveRecordRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsersIfEmpty();
        seedEmployeeDataIfEmpty();
        syncExistingEmployeeNames();
        seedDocumentsIfEmpty();
        syncExistingDocumentIndexingStatus();
        backfillChatConversations();
    }

    private void backfillChatConversations() {
        try {
            List<ChatMessage> allMessages = chatMessageRepository.findAll();
            if (allMessages == null || allMessages.isEmpty()) {
                return;
            }
            Map<String, List<ChatMessage>> byUser = allMessages.stream()
                    .collect(Collectors.groupingBy(ChatMessage::getUsername));

            for (Map.Entry<String, List<ChatMessage>> entry : byUser.entrySet()) {
                List<ChatMessage> msgs = entry.getValue();
                msgs.sort(Comparator.comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

                String currentConvId = null;
                LocalDateTime lastTime = null;

                for (ChatMessage msg : msgs) {
                    if (msg.getConversationId() != null && !msg.getConversationId().trim().isEmpty()) {
                        currentConvId = msg.getConversationId().trim();
                        lastTime = msg.getCreatedAt();
                        continue;
                    }
                    if (currentConvId != null && lastTime != null && msg.getCreatedAt() != null
                            && Duration.between(lastTime, msg.getCreatedAt()).toMinutes() <= 15) {
                        msg.setConversationId(currentConvId);
                    } else {
                        long ts = msg.getCreatedAt() != null ? msg.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis();
                        currentConvId = "conv_" + ts + "_" + msg.getId();
                        msg.setConversationId(currentConvId);
                    }
                    lastTime = msg.getCreatedAt();
                    chatMessageRepository.save(msg);
                }
            }
        } catch (Exception e) {
            System.err.println("Note on chat conversation backfill: " + e.getMessage());
        }
    }

    private void syncExistingEmployeeNames() {
        userRepository.findAll().forEach(u -> {
            if ("Arthur Administrator".equalsIgnoreCase(u.getFullName()) || "Arthur".equalsIgnoreCase(u.getFullName())) {
                u.setFullName("Ashwin Mani");
                userRepository.save(u);
            } else if ("Helen HR Manager".equalsIgnoreCase(u.getFullName()) || "Helen".equalsIgnoreCase(u.getFullName())) {
                u.setFullName("Suganthi Mani");
                userRepository.save(u);
            }
        });

        profileRepository.findByUsername("admin_user").ifPresent(p -> {
            p.setFullName("Ashwin Mani");
            profileRepository.save(p);
        });
        profileRepository.findByUsername("hr_user").ifPresent(p -> {
            p.setFullName("Suganthi Mani");
            profileRepository.save(p);
        });
        for (EmployeeProfile p : profileRepository.findAll()) {
            boolean changed = false;
            if ("Arthur Administrator".equalsIgnoreCase(p.getReportingManager()) || "Arthur".equalsIgnoreCase(p.getReportingManager())) {
                p.setReportingManager("Ashwin Mani");
                changed = true;
            }
            if ("Arthur Administrator".equalsIgnoreCase(p.getFullName()) || "Arthur".equalsIgnoreCase(p.getFullName())) {
                p.setFullName("Ashwin Mani");
                changed = true;
            } else if ("Helen HR Manager".equalsIgnoreCase(p.getFullName()) || "Helen".equalsIgnoreCase(p.getFullName())) {
                p.setFullName("Suganthi Mani");
                changed = true;
            }
            if (changed) {
                profileRepository.save(p);
            }
        }

        for (KnowledgeDocument doc : documentRepository.findAll()) {
            boolean changed = false;
            if ("Arthur Administrator".equalsIgnoreCase(doc.getOwner()) || "Arthur".equalsIgnoreCase(doc.getOwner())) {
                doc.setOwner("Ashwin Mani");
                changed = true;
            } else if ("Helen HR Manager".equalsIgnoreCase(doc.getOwner()) || "Helen".equalsIgnoreCase(doc.getOwner())) {
                doc.setOwner("Suganthi Mani");
                changed = true;
            }
            if (changed) {
                documentRepository.save(doc);
            }
        }
    }

    private void syncExistingDocumentIndexingStatus() {
        for (KnowledgeDocument doc : documentRepository.findAll()) {
            int chunks = chunkRepository.findByDocumentIdAndDocumentVersion(doc.getId(), doc.getVersion()).size();
            if (chunks > 0 && doc.getIndexingStatus() != IndexingStatus.INDEXED) {
                doc.setIndexingStatus(IndexingStatus.INDEXED);
                documentRepository.save(doc);
            }
        }
    }

    private void seedUsersIfEmpty() {
        String encodedPassword = passwordEncoder.encode("password123");

        userRepository.findByUsernameIgnoreCase("aswini").ifPresentOrElse(u -> {
            u.setUsername("aswini");
            u.setPassword(encodedPassword);
            u.setFullName("Aswini Mani");
            userRepository.save(u);
        }, () -> {
            userRepository.save(new UserAccount("aswini", encodedPassword, "Aswini Mani", UserRole.ROLE_EMPLOYEE));
        });

        userRepository.findByUsernameIgnoreCase("employee1").ifPresentOrElse(u -> {
            u.setPassword(encodedPassword);
            userRepository.save(u);
        }, () -> {
            userRepository.save(new UserAccount("employee1", encodedPassword, "Alice Employee", UserRole.ROLE_EMPLOYEE));
        });

        userRepository.findByUsernameIgnoreCase("hr_user").ifPresentOrElse(u -> {
            u.setPassword(encodedPassword);
            u.setFullName("Suganthi Mani");
            userRepository.save(u);
        }, () -> {
            userRepository.save(new UserAccount("hr_user", encodedPassword, "Suganthi Mani", UserRole.ROLE_HR));
        });

        userRepository.findByUsernameIgnoreCase("admin_user").ifPresentOrElse(u -> {
            u.setPassword(encodedPassword);
            u.setFullName("Ashwin Mani");
            userRepository.save(u);
        }, () -> {
            userRepository.save(new UserAccount("admin_user", encodedPassword, "Ashwin Mani", UserRole.ROLE_ADMIN));
        });
    }

    private void seedEmployeeDataIfEmpty() {
        if (profileRepository.count() == 0) {
            // 1. Employee Profiles
            profileRepository.save(new EmployeeProfile(
                    "aswini", "EMP-1001", "Aswini Mani", "aswini@company.internal",
                    "Engineering", "Senior Software Engineer", java.time.LocalDate.of(2023, 1, 15),
                    "Chennai (Hybrid)", "Ashwin Mani", AccessLevel.EMPLOYEE
            ));
            profileRepository.save(new EmployeeProfile(
                    "employee1", "EMP-1002", "Alice Employee", "alice@company.internal",
                    "Engineering", "Software Engineer", java.time.LocalDate.of(2023, 6, 1),
                    "Bangalore Tech Hub", "Ashwin Mani", AccessLevel.EMPLOYEE
            ));
            profileRepository.save(new EmployeeProfile(
                    "hr_user", "EMP-1003", "Suganthi Mani", "suganthi.hr@company.internal",
                    "Human Resources", "HR Operations Lead", java.time.LocalDate.of(2022, 3, 10),
                    "Chennai Office", "Ashwin Mani", AccessLevel.HR
            ));
            profileRepository.save(new EmployeeProfile(
                    "admin_user", "EMP-1000", "Ashwin Mani", "admin@company.internal",
                    "Administration", "Principal IT Administrator", java.time.LocalDate.of(2021, 8, 1),
                    "Headquarters", "Board of Directors", AccessLevel.ADMIN
            ));

            // 2. Employee Leave Records (2026)
            leaveRecordRepository.save(new EmployeeLeaveRecord(
                    "aswini", 2026, 25, 7, 6, 1, 10, 0, 24, 8, 3, 1, 8, 1
            ));
            leaveRecordRepository.save(new EmployeeLeaveRecord(
                    "employee1", 2026, 25, 5, 6, 2, 10, 2, 24, 12, 2, 0, 7, 0
            ));
            leaveRecordRepository.save(new EmployeeLeaveRecord(
                    "hr_user", 2026, 25, 10, 6, 3, 10, 1, 24, 6, 4, 2, 13, 0
            ));
            leaveRecordRepository.save(new EmployeeLeaveRecord(
                    "admin_user", 2026, 25, 4, 6, 0, 10, 0, 24, 4, 5, 1, 5, 0
            ));

            // 3. Employee Leave Requests
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "aswini", "Aswini Mani", "Planned Leave",
                    java.time.LocalDate.of(2026, 10, 20), java.time.LocalDate.of(2026, 10, 21),
                    2, "Family travel and personal commitment", "PENDING"
            ));
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "aswini", "Aswini Mani", "Casual Leave",
                    java.time.LocalDate.of(2026, 9, 12), java.time.LocalDate.of(2026, 9, 12),
                    1, "Personal emergency", "APPROVED"
            ));
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "aswini", "Aswini Mani", "Planned Leave",
                    java.time.LocalDate.of(2026, 7, 7), java.time.LocalDate.of(2026, 7, 11),
                    5, "Summer annual leave", "APPROVED"
            ));
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "aswini", "Aswini Mani", "Work From Home (WFH)",
                    java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 8, 8),
                    8, "Remote sprint delivery", "APPROVED"
            ));
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "employee1", "Alice Employee", "Sick Leave",
                    java.time.LocalDate.of(2026, 5, 15), java.time.LocalDate.of(2026, 5, 16),
                    2, "Medical recovery", "APPROVED"
            ));
            leaveRequestRepository.save(new EmployeeLeaveRequest(
                    "employee1", "Alice Employee", "Planned Leave",
                    java.time.LocalDate.of(2026, 6, 10), java.time.LocalDate.of(2026, 6, 14),
                    5, "Mid-year vacation", "APPROVED"
            ));
        }
    }

    private void seedDocumentsIfEmpty() {
        if (documentRepository.count() == 0) {
            // Document 1: Leave Policy (Access Level: EMPLOYEE)
            KnowledgeDocument doc1 = new KnowledgeDocument(
                    "Company Annual Leave and Time Off Policy",
                    "Comprehensive annual leave guidelines, statutory sick pay, and carry-over rules.",
                    DocumentCategory.POLICY,
                    "hr_user",
                    AccessLevel.EMPLOYEE,
                    "annual_leave_policy.txt",
                    "text/plain",
                    "Full-time regular employees are entitled to 25 days of paid annual leave per calendar year. Leave must be requested through the HR portal at least 2 weeks in advance. Employees are permitted to carry over a maximum of 5 unused annual leave days into the subsequent calendar year. Sick leave provides up to 10 paid days annually with a doctor note required after 3 consecutive days.",
                    null
            );
            doc1 = documentRepository.save(doc1);

            chunkRepository.save(new KnowledgeChunk(doc1.getId(), 1, 1, "Section 1: Annual Leave Entitlement", 1,
                    "Full-time regular employees are entitled to 25 days of paid annual leave per calendar year. Leave must be requested through the HR portal at least 2 weeks in advance of the planned start date."));
            chunkRepository.save(new KnowledgeChunk(doc1.getId(), 1, 2, "Section 2: Carry-Over Policy", 1,
                    "Employees are permitted to carry over a maximum of 5 unused annual leave days into the subsequent calendar year. Carried-over days must be utilized before March 31st of the new year."));
            chunkRepository.save(new KnowledgeChunk(doc1.getId(), 1, 3, "Section 3: Sick Leave Regulations", 2,
                    "Employees receive up to 10 days of paid sick leave annually. A certified medical practitioner note is mandatory for consecutive absences exceeding 3 days."));

            // Document 2: Incident Runbook (Access Level: EMPLOYEE)
            KnowledgeDocument doc2 = new KnowledgeDocument(
                    "Production Incident Severity Runbook",
                    "Operational instructions for diagnosing and handling Severity 1 and Severity 2 system outages.",
                    DocumentCategory.TECHNICAL_RUNBOOK,
                    "admin_user",
                    AccessLevel.EMPLOYEE,
                    "incident_runbook.txt",
                    "text/plain",
                    "A Severity 1 incident indicates complete service downtime. The on-call Site Reliability Engineer must initiate a virtual war room within 10 minutes of alert trigger. Root Cause Analysis (RCA) must be published within 48 hours.",
                    null
            );
            doc2 = documentRepository.save(doc2);

            chunkRepository.save(new KnowledgeChunk(doc2.getId(), 1, 1, "Section 1: Severity 1 Protocol", 1,
                    "A Severity 1 incident indicates complete service downtime. The on-call Site Reliability Engineer must initiate a virtual war room within 10 minutes of alert trigger and notify stakeholders."));
            chunkRepository.save(new KnowledgeChunk(doc2.getId(), 1, 2, "Section 2: Post-Mortem and RCA", 2,
                    "Following incident resolution, a comprehensive Root Cause Analysis (RCA) document must be drafted and published to the engineering knowledge base within 48 hours."));

            // Document 3: Executive Compensation (Access Level: HR)
            KnowledgeDocument doc3 = new KnowledgeDocument(
                    "HR Executive Compensation and Bonus Structure",
                    "Confidential executive performance appraisal metrics, stock options, and bonus brackets.",
                    DocumentCategory.POLICY,
                    "hr_user",
                    AccessLevel.HR,
                    "executive_compensation.txt",
                    "text/plain",
                    "Executive annual performance bonuses are tiered from 15% to 35% of base salary, contingent upon audited corporate EBITDA benchmarks.",
                    null
            );
            doc3 = documentRepository.save(doc3);

            chunkRepository.save(new KnowledgeChunk(doc3.getId(), 1, 1, "Section 1: Bonus Calculation", 1,
                    "Executive annual performance bonuses are tiered from 15% to 35% of base salary, contingent upon audited corporate EBITDA benchmarks and department key performance indicators."));

            // Document 4: Bastion Protocols (Access Level: ADMIN)
            KnowledgeDocument doc4 = new KnowledgeDocument(
                    "Admin Infrastructure Credentials and Bastion Host Protocols",
                    "Super-admin access, bastion host SSH gateway instructions, and disaster recovery procedures.",
                    DocumentCategory.TECHNICAL_RUNBOOK,
                    "admin_user",
                    AccessLevel.ADMIN,
                    "admin_bastion_protocols.txt",
                    "text/plain",
                    "Super-admin bastion access requires hardware FIDO2 token verification. All shell sessions are logged to an immutable compliance bucket.",
                    null
            );
            doc4 = documentRepository.save(doc4);

            chunkRepository.save(new KnowledgeChunk(doc4.getId(), 1, 1, "Section 1: Bastion Access", 1,
                    "Super-admin bastion access requires hardware FIDO2 token verification. All shell sessions are logged to an immutable compliance bucket."));

            // Document 5: Remote Work FAQs (Access Level: PUBLIC)
            KnowledgeDocument doc5 = new KnowledgeDocument(
                    "Remote Work Equipment and Expense FAQs",
                    "Frequently asked questions regarding monitor allowances, remote work stipend, and expense reports.",
                    DocumentCategory.FAQ,
                    "hr_user",
                    AccessLevel.PUBLIC,
                    "remote_work_faq.txt",
                    "text/plain",
                    "Eligible remote and hybrid employees can claim up to $500 for home office equipment including monitors and ergonomic chairs. Monthly broadband stipend is $50.",
                    null
            );
            doc5 = documentRepository.save(doc5);

            chunkRepository.save(new KnowledgeChunk(doc5.getId(), 1, 1, "Section 1: Hardware Allowance", 1,
                    "Eligible remote and hybrid employees can claim up to $500 for home office equipment including monitors, keyboard, and ergonomic chairs upon submitting receipts."));
            chunkRepository.save(new KnowledgeChunk(doc5.getId(), 1, 2, "Section 2: Internet Stipend", 1,
                    "A monthly internet allowance of $50 is credited directly via payroll upon submitting monthly utility statements."));

            // Seed initial feedback and search history
            feedbackRepository.save(new KnowledgeFeedback(doc1.getId(), "How many days of annual leave are available?", "ANS-POLICY-001", 5, "Clear citation of the 25 days policy.", "employee1"));
            feedbackRepository.save(new KnowledgeFeedback(doc5.getId(), "What is the home office allowance limit?", "ANS-FAQ-002", 4, "Helpful answer regarding the $500 equipment reimbursement.", "employee1"));

            searchHistoryRepository.save(new SearchHistory("annual leave days", "POLICY", 3, "employee1"));
            searchHistoryRepository.save(new SearchHistory("incident war room", "TECHNICAL_RUNBOOK", 1, "employee1"));
            searchHistoryRepository.save(new SearchHistory("remote work monitor", "FAQ", 2, "employee1"));
        }
    }
}
