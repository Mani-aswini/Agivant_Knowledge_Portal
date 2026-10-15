package com.example.knowledgeportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Employee Account Self-Registration.
 */
public class RegisterRequest {

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-z][a-z0-9._]{2,29}$",
            message = "Username must start with a lowercase letter, contain only lowercase letters, digits, dots or underscores (3-30 characters), and cannot be numbers only")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,100}$",
            message = "Password must be at least 6 characters long and contain a mix of letters and numbers/symbols")
    private String password;

    @NotBlank(message = "Full name cannot be empty")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-zA-Z].*[a-zA-Z])[a-zA-Z\\s.'-]+$",
            message = "Full Name must contain only alphabetic letters, spaces, and standard name characters (cannot be numbers or numeric only)")
    private String fullName;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
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
}
