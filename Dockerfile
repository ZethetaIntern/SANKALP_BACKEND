FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy all project files
COPY . .

# Convert Windows line endings (CRLF) to Unix line endings (LF) for mvnw
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Build the Spring Boot executable jar
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal, fast, secure JRE runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render assigns dynamic PORT environment variable (defaults to 8080)
EXPOSE 8080
ENV PORT=8080

# Run Spring Boot passing dynamic Render PORT
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
