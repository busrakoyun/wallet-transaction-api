# syntax=docker/dockerfile:1

# ---- Build stage: compile and package with JDK 21 + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached unless the POM changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Build the application (tests run in CI / via `./mvnw clean install`).
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
