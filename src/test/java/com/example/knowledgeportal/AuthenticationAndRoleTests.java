package com.example.knowledgeportal;

import com.example.knowledgeportal.dto.LoginRequest;
import com.example.knowledgeportal.dto.RegisterRequest;
import com.example.knowledgeportal.dto.UserResponse;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationAndRoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    @DisplayName("TC-06-01: Employee self-registration creates valid ROLE_EMPLOYEE account and session")
    void testEmployeeRegistration() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("new_employee_bob", "password123", "Bob Newbie");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_employee_bob"))
                .andExpect(jsonPath("$.fullName").value("Bob Newbie"))
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYEE"));

        assertTrue(userAccountRepository.existsByUsername("new_employee_bob"),
                "Newly registered employee account must exist in repository.");
        assertEquals(UserRole.ROLE_EMPLOYEE,
                userAccountRepository.findByUsername("new_employee_bob").orElseThrow().getRole());
    }

    @Test
    @Transactional
    @DisplayName("TC-06-02: Registering with existing username rejects with HTTP 409 Conflict")
    void testDuplicateUsernameRegistrationFails() throws Exception {
        // employee1 already seeded in DataInitializer
        RegisterRequest duplicateReq = new RegisterRequest("employee1", "somepass123", "Duplicate Alice");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @Transactional
    @DisplayName("TC-06-03: Administrator gateway authenticates valid ROLE_ADMIN credentials")
    void testAdminGatewaySuccess() throws Exception {
        LoginRequest adminReq = new LoginRequest("admin_user", "password123");

        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin_user"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @Transactional
    @DisplayName("TC-06-04: Administrator gateway rejects non-admin employee credentials with HTTP 403 Forbidden")
    void testAdminGatewayRejectsEmployee() throws Exception {
        LoginRequest empAttempt = new LoginRequest("employee1", "password123");

        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empAttempt)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Administrator privileges required")));
    }

    @Test
    @Transactional
    @DisplayName("TC-06-05: Standard Employee login authenticates successfully with ROLE_EMPLOYEE")
    void testEmployeeLoginSuccess() throws Exception {
        LoginRequest empReq = new LoginRequest("employee1", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee1"))
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYEE"));
    }

    @Test
    @Transactional
    @DisplayName("TC-06-06: Mismatched-case username (e.g. Employee1 instead of employee1) rejects authentication with HTTP 401 Unauthorized")
    void testMismatchedCaseUsernameRejected() throws Exception {
        LoginRequest empReq = new LoginRequest("Employee1", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid username and password")));
    }
}
