# Étape 1: Build l'app avec Gradle
FROM gradle:8.5-jdk17 AS build
COPY backend /app
WORKDIR /app
RUN gradle clean build -x test

# Étape 2: Image finale légère avec JDK
FROM eclipse-temurin:17-jre-jammy
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]