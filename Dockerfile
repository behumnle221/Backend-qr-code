# Étape 1: Build l'app avec Gradle
FROM gradle:8.5-jdk17 AS build
COPY backend /app
WORKDIR /app
RUN gradle clean build -x test

# Étape 2: Image finale légère avec JDK
FROM eclipse-temurin:17-jre-jammy
COPY --from=build /app/build/libs/*.jar /app/
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app/*.jar"]