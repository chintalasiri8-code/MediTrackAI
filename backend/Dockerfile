# Build Stage: Official Maven image with Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cap Maven memory usage on Free Tier build runners
ENV MAVEN_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx768m"

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production jar without tests (finalName is configured as 'app' in pom.xml)
RUN mvn clean package -Dmaven.test.skip=true -DskipTests

# Runtime Stage: Lightweight Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy exact built artifact directly
COPY --from=build /app/target/app.jar /app/app.jar

# Render default port support (Render injects PORT=10000 or custom)
ENV PORT=8080
EXPOSE 8080 10000

# Strict JVM memory tuning for Render Free Tier (512MB RAM cap)
CMD ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx350m -Xms128m -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
