package com.example.knowledgeportal.security;

import com.example.knowledgeportal.entity.AccessLevel;
import com.example.knowledgeportal.entity.DocumentStatus;
import com.example.knowledgeportal.entity.KnowledgeDocument;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.exception.UnauthorizedAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Centralized Permission Service.
 * 
 * Enforces business access control rules in pure Java independently of the AI model.
 * 
 * Rules:
 * 1. ROLE_EMPLOYEE has clearance for: PUBLIC, EMPLOYEE
 * 2. ROLE_HR has clearance for: PUBLIC, EMPLOYEE, HR
 * 3. ROLE_ADMIN has clearance for: PUBLIC, EMPLOYEE, HR, ADMIN
 * 
 * Crucial Safeguard:
 * Document clearance is strictly evaluated before any snippet, full text, or citation
 * is sent to the search result response or supplied to the AI model.
 */
@Service
public class PermissionService {

    /**
     * Computes the set of allowable document AccessLevels based on the user's role.
     */
    public Set<AccessLevel> getAllowedAccessLevels(Authentication auth) {
        UserRole role = getCurrentUserRole(auth);

        switch (role) {
            case ROLE_ADMIN:
                return EnumSet.of(AccessLevel.PUBLIC, AccessLevel.EMPLOYEE, AccessLevel.HR, AccessLevel.ADMIN);
            case ROLE_HR:
                return EnumSet.of(AccessLevel.PUBLIC, AccessLevel.EMPLOYEE, AccessLevel.HR);
            case ROLE_EMPLOYEE:
            default:
                return EnumSet.of(AccessLevel.PUBLIC, AccessLevel.EMPLOYEE);
        }
    }

    /**
     * Checks if the authenticated user is permitted to read the given document.
     */
    public boolean canAccessDocument(Authentication auth, KnowledgeDocument document) {
        if (document == null) {
            return false;
        }

        // Archived documents are only accessible to ADMIN or the original owner
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            return isUserAdmin(auth) || isDocumentOwner(auth, document);
        }

        Set<AccessLevel> allowedLevels = getAllowedAccessLevels(auth);
        return allowedLevels.contains(document.getAccessLevel());
    }

    /**
     * Validates access and throws UnauthorizedAccessException if forbidden.
     */
    public void validateDocumentAccess(Authentication auth, KnowledgeDocument document) {
        if (!canAccessDocument(auth, document)) {
            throw new UnauthorizedAccessException("Access Denied: You do not have permission to view document '" +
                    document.getTitle() + "' (Access Level: " + document.getAccessLevel() + ")");
        }
    }

    /**
     * Checks if the user is authorized to perform administrative actions (archive, reindex).
     */
    public boolean canManageDocument(Authentication auth, KnowledgeDocument document) {
        if (document == null) {
            return false;
        }
        return isUserAdmin(auth) || isDocumentOwner(auth, document);
    }

    /**
     * Validates administrative permission and throws UnauthorizedAccessException if forbidden.
     */
    public void validateManageDocument(Authentication auth, KnowledgeDocument document) {
        if (!canManageDocument(auth, document)) {
            throw new UnauthorizedAccessException("Administrative Access Denied: Only ADMIN or document owner can modify this document.");
        }
    }

    /**
     * Extracts username from the Spring Security Authentication context.
     */
    public String getCurrentUsername(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "anonymousUser";
        }
        return auth.getName();
    }

    /**
     * Resolves the user's highest role from their granted authorities.
     */
    public UserRole getCurrentUserRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return UserRole.ROLE_EMPLOYEE;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String authName = authority.getAuthority();
            if (UserRole.ROLE_ADMIN.name().equals(authName)) {
                return UserRole.ROLE_ADMIN;
            } else if (UserRole.ROLE_HR.name().equals(authName)) {
                return UserRole.ROLE_HR;
            }
        }
        return UserRole.ROLE_EMPLOYEE;
    }

    public boolean isUserAdmin(Authentication auth) {
        return getCurrentUserRole(auth) == UserRole.ROLE_ADMIN;
    }

    public boolean isDocumentOwner(Authentication auth, KnowledgeDocument document) {
        String currentUsername = getCurrentUsername(auth);
        return currentUsername.equalsIgnoreCase(document.getOwner());
    }
}
