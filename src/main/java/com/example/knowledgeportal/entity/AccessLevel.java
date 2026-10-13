package com.example.knowledgeportal.entity;

/**
 * Access clearance levels for documents.
 * Hierarchical visibility:
 * - PUBLIC: Accessible to all authenticated users.
 * - EMPLOYEE: Accessible to regular employees, HR, and Admins.
 * - HR: Restricted to Human Resources personnel and Admins.
 * - ADMIN: Restricted strictly to System Administrators.
 */
public enum AccessLevel {
    PUBLIC,
    EMPLOYEE,
    HR,
    ADMIN
}
