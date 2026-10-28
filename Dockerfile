# Step 1: Build the application (Maven + JDK 25)
FROM maven:3-eclipse-temurin-25-alpine AS builder
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build JAR
COPY src ./src
RUN mvn package -DskipTests

# Step 2: Create a minimal runtime image (JRE + JDK 25)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]