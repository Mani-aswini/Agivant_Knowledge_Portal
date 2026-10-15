# ===================================================================
# Stage 1: Build the Spring Boot application using Maven & Java 21
# ===================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code and static web assets
COPY src ./src

# Package production executable fat JAR (skipping unit tests for fast container builds)
RUN mvn clean package -DskipTests -B

# ===================================================================
# Stage 2: Minimal, secure Java 21 JRE runtime for Render
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged system group and user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create persistent uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

# Render injects PORT dynamically (typically 10000 or custom)
ENV PORT=8080
ENV SERVER_PORT=8080
EXPOSE ${PORT}

# Run as non-root user
USER appuser

# Container-aware JVM memory settings and fast startup entropy
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
