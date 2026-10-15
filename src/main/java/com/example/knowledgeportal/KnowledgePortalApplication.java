package com.example.knowledgeportal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main entry point for the Internal Knowledge Portal with AI Research Agent.
 * 
 * @SpringBootApplication enables:
 * 1. @Configuration: Tags the class as a source of bean definitions.
 * 2. @EnableAutoConfiguration: Tells Spring Boot to configure beans based on classpath dependencies.
 * 3. @ComponentScan: Scans for Spring components, services, and controllers under this package.
 */
@SpringBootApplication
public class KnowledgePortalApplication {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePortalApplication.class);

    public static void main(String[] args) {
        sanitizeEnvironmentProperties();
        loadDotEnvIfPresent();
        SpringApplication.run(KnowledgePortalApplication.class, args);
    }

    /**
     * Sanitizes critical environment variables to strip accidental carriage returns,
     * newlines, or trailing whitespace introduced when copying values into hosting providers like Render.
     */
    public static void sanitizeEnvironmentProperties() {
        String[] keys = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "PORT", "AI_API_KEY"};
        for (String key : keys) {
            String val = System.getenv(key);
            if (val != null) {
                String[] lines = val.split("[\\r\\n]+");
                for (String line : lines) {
                    String cleaned = line.trim();
                    if (!cleaned.isEmpty()) {
                        System.setProperty(key, cleaned);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Automatically loads key-value pairs from a local .env file in the working directory
     * into System properties if they are not already set in the operating system environment.
     * This provides seamless local development while keeping secrets out of source control.
     */
    public static void loadDotEnvIfPresent() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            int loadedCount = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx > 0) {
                    String key = trimmed.substring(0, eqIdx).trim();
                    String value = trimmed.substring(eqIdx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
            }
            if (loadedCount > 0) {
                log.info("Loaded {} environment properties from local .env file", loadedCount);
            }
        } catch (IOException e) {
            log.warn("Could not read local .env file: {}", e.getMessage());
        }
    }
}

