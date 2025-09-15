# Hi-Doc API Service Dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

# Expect the jar built as target/hi-doc-api-service.jar
COPY target/hi-doc-api-service.jar app.jar

EXPOSE 8080

# JVM opts can be overridden at runtime
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
