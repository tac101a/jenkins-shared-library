# ==========================================
# STAGE 1: BUILDER
# ==========================================
# Use a Maven and JDK 17 base image
FROM docker.io/eclipse-temurin:17-jdk-jammy AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven configuration first to optimize Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies in batch mode to reduce interactive output
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package the application with tests skipped in batch mode
RUN ./mvnw package -DskipTests -B

# ==========================================
# STAGE 2: RUNTIME
# ==========================================
# Use a lightweight JRE image for runtime
FROM docker.io/eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a non-root user for secure application execution
RUN addgroup --system spring && adduser --system --ingroup spring springuser
USER springuser

# Optimize JVM memory limits for the container
ENV JAVA_OPTS="-Xmx256m -Xms256m"

# Copy the packaged application from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the default Spring Petclinic application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
