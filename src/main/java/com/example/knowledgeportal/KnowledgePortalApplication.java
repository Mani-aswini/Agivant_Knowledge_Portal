package com.example.knowledgeportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

    public static void main(String[] args) {
        SpringApplication.run(KnowledgePortalApplication.class, args);
    }
}
