# Build stage (Gradle)
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy only Gradle wrapper + build scripts first for better layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Pre-download dependencies (cacheable)
RUN ./gradlew --no-daemon dependencies

# Copy source
COPY src ./src

# Build executable jar
RUN ./gradlew --no-daemon clean bootJar -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/app.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
