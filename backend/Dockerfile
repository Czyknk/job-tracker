# Java version for both stages; docker-compose.yml passes it as a build argument (default 25, as in pom.xml)
ARG JAVA_VERSION=25

# Step 1: Build the application (Maven + JDK)
FROM maven:3-eclipse-temurin-${JAVA_VERSION}-alpine AS builder
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build JAR
COPY src ./src
RUN mvn package -DskipTests

# Step 2: Create a minimal runtime image (JRE only)
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]