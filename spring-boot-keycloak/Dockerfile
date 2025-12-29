# Simplified Dockerfile using an existing local base image to bypass pull issues
# Using an older backend image that we know is present on the system
FROM angularproject-spring-api:latest
WORKDIR /app

# The existing image already has the right environment
# We are just replacing the JAR with our new one containing the CORS fix
COPY target/keycloak-integration-1.0.0-exec.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

