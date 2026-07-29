FROM maven:3-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

COPY src/ ./src/

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Plugins directory for Spring Boot PropertiesLauncher plugin loading.
# Pre-built plugin JARs (if any are flattened into plugins/ at build time) are
# baked into the image for self-contained deployment. Copying the directory
# (rather than a plugins/*.jar glob) keeps the build working when no plugin
# jars are present yet — see .dockerignore for what's excluded from this copy.
COPY plugins/ /app/plugins/
ENV LOADER_PATH=/app/plugins

EXPOSE 17922

# Use PropertiesLauncher to support runtime plugin loading via loader.path
ENTRYPOINT ["java", "-cp", "app.jar", "org.springframework.boot.loader.launch.PropertiesLauncher"]
