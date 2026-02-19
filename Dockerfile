# ====================== BUILD STAGE ======================
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copie tout le dossier backend (qui contient gradlew, build.gradle, src, etc.)
COPY backend /app

# 🔥 IMPORTANT : Donne les droits d'exécution au Gradle Wrapper
RUN chmod +x ./gradlew

# Build propre
RUN ./gradlew clean bootJar -x test --no-daemon

# ====================== RUNTIME STAGE ======================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copie le JAR (nom fixe grâce à bootJar dans build.gradle)
COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]