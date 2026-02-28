# --- Étape 1 : Construction de l'application ---
# Utilise une image Gradle avec JDK pour compiler le projet.
FROM gradle:8.14-jdk17 AS build
WORKDIR /app

# Copie les fichiers de build Gradle et le code source
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Exécute la commande Gradle pour construire le JAR exécutable
# Cela générera le fichier JAR dans build/libs
RUN gradle build --no-daemon -x test

# --- Étape 2 : Exécution de l'application ---
# Utilise une image JDK minimale et légère pour l'exécution (plus sécurisée et petite)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copie le JAR construit depuis l'étape 'build' vers l'étape 'run'
COPY --from=build /app/build/libs/*.jar app.jar

# Expose le port par défaut de Spring Boot
EXPOSE 8081

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
