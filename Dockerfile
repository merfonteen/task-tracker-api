FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/task-tracker-api-0.0.1-SNAPSHOT.jar task-tracker.jar

ENTRYPOINT ["java", "-jar", "task-tracker.jar"]