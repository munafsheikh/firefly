FROM maven:3-eclipse-temurin-26 AS builder

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

COPY src/ ./src/

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:26-jre AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
