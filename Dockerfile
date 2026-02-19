# ====================== BUILD STAGE ======================
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY backend /app

# Build avec le nom fixe
RUN ./gradlew clean bootJar -x test --no-daemon

# ====================== RUNTIME STAGE ======================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copie le fichier avec le nom fixe que nous venons de définir
COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]