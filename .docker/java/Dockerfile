FROM maven:3-eclipse-temurin-25@sha256:047a82fc82c613266cfb1b66fd920be9c8ba51fd67edc68245e4542768f45e36 AS builder

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

COPY src/ ./src/

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre@sha256:ecdc0adb37bde1a281c4d245a3cd535004b1fc8322d560f5b09463b248c81df0 AS runtime

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
