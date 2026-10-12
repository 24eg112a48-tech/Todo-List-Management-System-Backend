# Production image for the Todo backend (used by container hosts, e.g. Render).
# Local development still uses ./mvnw spring-boot:run (no Docker required).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/todo-list-*.jar app.jar
# Render (and similar hosts) inject $PORT; Spring reads it via server.port=${PORT:8080}.
# H2 file path and CORS origins are likewise env-driven (see application.properties).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
