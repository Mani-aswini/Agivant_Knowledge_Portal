package com.example.knowledgeportal.config;

import com.example.knowledgeportal.entity.*;
import com.example.knowledgeportal.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userRepository,
                           KnowledgeDocumentRepository documentRepository,
                           KnowledgeChunkRepository chunkRepository,
                           KnowledgeFeedbackRepository feedbackRepository,
                           SearchHistoryRepository searchHistoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.feedbackRepository = feedbackRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsersIfEmpty();
        seedDocumentsIfEmpty();
    }

    private void seedUsersIfEmpty() {
        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("password123");

            userRepository.save(new UserAccount("employee1", encodedPassword, "Alice Employee", UserRole.ROLE_EMPLOYEE));
            userRepository.save(new UserAccount("hr_user", encodedPassword, "Helen HR Manager", UserRole.ROLE_HR));
            userRepository.save(new UserAccount("admin_user", encodedPassword, "Arthur Administrator", UserRole.ROLE_ADMIN));
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
