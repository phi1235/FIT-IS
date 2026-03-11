FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/gateway-service-1.0.0.jar /app/app.jar
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
