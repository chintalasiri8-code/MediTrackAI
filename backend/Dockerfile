# Build Stage: Official Maven image with Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production jar without tests
RUN mvn clean package -DskipTests

# Runtime Stage: Lightweight Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Render default port support (Render injects PORT=10000)
ENV PORT=8080
EXPOSE 8080 10000

# Strict JVM memory tuning for Render Free Tier (512MB RAM cap)
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx350m -Xms128m -Dserver.port=${PORT:-8080} -jar app.jar"]
