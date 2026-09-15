# Mehrstufiger Build: bauen mit Maven, ausliefern nur mit einer JRE.
# Das fertige Image traegt dadurch weder Maven noch den Quellcode mit sich herum.

# ---------- Stufe 1: bauen ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Erst nur die pom.xml kopieren und die Abhaengigkeiten laden. Solange sich die
# pom.xml nicht aendert, kommt dieser Layer aus dem Cache - ein Codewechsel
# loest dann keinen erneuten Download aus.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY lombok.config .
COPY src ./src
# Tests laufen in der Pipeline, nicht im Image-Build.
RUN mvn -B -q clean package -DskipTests

# ---------- Stufe 2: ausliefern ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Nicht als root laufen lassen.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
USER spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
