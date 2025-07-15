# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first (better Docker caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Copy the JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]