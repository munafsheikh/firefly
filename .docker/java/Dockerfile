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

# Plugins directory for Spring Boot PropertiesLauncher plugin loading
RUN mkdir -p /app/plugins
ENV LOADER_PATH=/app/plugins

EXPOSE 17922

# Use PropertiesLauncher to support runtime plugin loading via loader.path
ENTRYPOINT ["java", "-cp", "app.jar", "org.springframework.boot.loader.launch.PropertiesLauncher"]
