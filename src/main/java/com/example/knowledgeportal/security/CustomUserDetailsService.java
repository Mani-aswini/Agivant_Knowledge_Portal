package com.example.knowledgeportal.security;

import com.example.knowledgeportal.entity.UserAccount;
import com.example.knowledgeportal.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService loads user authentication details directly from MySQL database.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }
        String trimmed = username.trim();
        UserAccount user = userAccountRepository.findByUsername(trimmed)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + trimmed));

        // Enforce strict case-sensitive matching:
        // Even if the SQL collation is case-insensitive, the stored username must match exact case.
        if (!user.getUsername().equals(trimmed)) {
            throw new UsernameNotFoundException("Invalid username: username is case-sensitive and must match exactly.");
        }

        return new UserPrincipal(user);
    }
}
