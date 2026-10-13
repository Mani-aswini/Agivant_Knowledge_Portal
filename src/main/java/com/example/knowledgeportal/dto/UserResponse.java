package com.example.knowledgeportal.dto;

import com.example.knowledgeportal.entity.UserRole;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private UserRole role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String fullName, UserRole role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
