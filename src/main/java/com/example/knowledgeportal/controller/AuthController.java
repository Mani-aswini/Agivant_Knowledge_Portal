package com.example.knowledgeportal.controller;

import com.example.knowledgeportal.dto.LoginRequest;
import com.example.knowledgeportal.dto.RegisterRequest;
import com.example.knowledgeportal.dto.UserResponse;
import com.example.knowledgeportal.entity.UserAccount;
import com.example.knowledgeportal.entity.UserRole;
import com.example.knowledgeportal.exception.ErrorResponse;
import com.example.knowledgeportal.repository.UserAccountRepository;
import com.example.knowledgeportal.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for User Authentication, Registration, and Session Management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Standard Login (Used by Employee login and unified authentication).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Invalid username and password.",
                    request.getRequestURI()
            ));
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new UserResponse(principal.getId(), principal.getUsername(),
                principal.getFullName(), principal.getRole()));
    }

    /**
     * Dedicated Admin Login. Strictly verifies that the authenticated user has ROLE_ADMIN.
     */
    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody LoginRequest loginRequest,
                                        HttpServletRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Invalid username and password.",
                    request.getRequestURI()
            ));
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getRole() != UserRole.ROLE_ADMIN) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "Access Denied",
                    "Administrator privileges required. Account '" + principal.getUsername() + "' (" + principal.getRole() + ") is not authorized for the Administrator Portal. Please use Employee Sign-In.",
                    "/api/auth/admin-login"
            ));
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.ok(new UserResponse(principal.getId(), principal.getUsername(),
                principal.getFullName(), principal.getRole()));
    }

    /**
     * Employee Registration. Self-registers a new employee account with ROLE_EMPLOYEE.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest,
                                      HttpServletRequest request) {
        if (userAccountRepository.existsByUsername(registerRequest.getUsername().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    "Username '" + registerRequest.getUsername().trim() + "' is already registered. Please choose another username or sign in.",
                    "/api/auth/register"
            ));
        }

        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        UserAccount newUser = new UserAccount(
                registerRequest.getUsername().trim(),
                encodedPassword,
                registerRequest.getFullName().trim(),
                UserRole.ROLE_EMPLOYEE
        );
        UserAccount savedUser = userAccountRepository.save(newUser);

        // Automatically authenticate the registered employee
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getUsername().trim(), registerRequest.getPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(
                savedUser.getId(), savedUser.getUsername(), savedUser.getFullName(), savedUser.getRole()
        ));
    }

    /**
     * Retrieves currently authenticated user details.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current authenticated user not found in database"));

        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(),
                user.getFullName(), user.getRole()));
    }

    /**
     * List all users for the Admin Directory view.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listAllUsers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getRole() != UserRole.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserResponse> users = userAccountRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Terminates the current user session.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Successfully logged out");
    }
}
