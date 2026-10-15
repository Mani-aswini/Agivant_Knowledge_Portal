package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for UserAccount entities.
 * 
 * Beginner Concept:
 * By extending JpaRepository<UserAccount, Long>, Spring Data JPA automatically
 * generates implementation methods for CRUD operations (save, findById, findAll, delete)
 * without writing any SQL queries manually.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
