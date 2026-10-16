package com.example.knowledgeportal;

import com.example.knowledgeportal.dto.CreateEmployeeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmployeeCreationValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateEmployeeRequest createValidRequest() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setUsername("deepa.nair");
        req.setPassword("SecurePass2026!");
        req.setFullName("Deepa Nair");
        req.setEmail("deepa.nair@company.internal");
        req.setDepartment("Engineering");
        req.setDesignation("Software Engineer");
        req.setReportingManager("Ashwin Mani");
        req.setRole("ROLE_EMPLOYEE");
        return req;
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should successfully create employee when all inputs adhere to validation rules")
    void testValidEmployeeCreation() throws Exception {
        CreateEmployeeRequest req = createValidRequest();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("deepa.nair"))
                .andExpect(jsonPath("$.fullName").value("Deepa Nair"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject numeric-only Full Name")
    void testNumericOnlyFullNameRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("valid.user");
        req.setFullName("12345");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Full Name")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject Full Name containing numbers")
    void testFullNameWithNumbersRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("valid.user");
        req.setFullName("Deepa 123");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Full Name")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject numeric-only Username")
    void testNumericOnlyUsernameRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("12345");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject uppercase Username")
    void testUppercaseUsernameRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("RahulSharma");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject password shorter than 6 characters or lacking mixed characters")
    void testWeakPasswordRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("valid.user");
        req.setPassword("12345"); // Too short and no letters

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Password")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject numbers in Reporting Manager name")
    void testReportingManagerWithNumbersRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("valid.user");
        req.setReportingManager("Manager 99");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Reporting Manager")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should reject corporate email not ending with @company.internal")
    void testInvalidCorporateEmailRejected() throws Exception {
        CreateEmployeeRequest req = createValidRequest();
        req.setUsername("valid.user");
        req.setEmail("invalid.email@gmail.com");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email")));
    }
}
