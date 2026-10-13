package com.example.knowledgeportal.ai;

import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.UserRole;

import java.util.Set;

/**
 * Encapsulates authenticated caller context for tool calls.
 * Ensures security credentials and roles are verified in Java before tool execution.
 */
public class UserContext {

    private final String username;
    private final UserRole role;
    private final Set<AccessLevel> allowedAccessLevels;

    public UserContext(String username, UserRole role, Set<AccessLevel> allowedAccessLevels) {
        this.username = username;
        this.role = role;
        this.allowedAccessLevels = allowedAccessLevels;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public Set<AccessLevel> getAllowedAccessLevels() {
        return allowedAccessLevels;
    }
}
