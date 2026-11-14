FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENV TRANSCRIPTION_SERVICE_URL=http://transcription-service:8000

ENTRYPOINT ["java", "-jar", "app.jar"]