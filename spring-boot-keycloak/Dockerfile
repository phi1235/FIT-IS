FROM angularproject-spring-api:latest
WORKDIR /app

COPY target/keycloak-integration-1.0.0-exec.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

