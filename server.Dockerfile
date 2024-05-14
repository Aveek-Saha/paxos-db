# Build stage
FROM maven:3.9.6-eclipse-temurin-11-alpine AS build
COPY . /app
WORKDIR /app/server
RUN mvn clean install package

# Run stage
FROM amazoncorretto:11-alpine3.19 AS server-build
COPY --from=build /app/server/target/server-4.0-jar-with-dependencies.jar /app/server.jar
# ENTRYPOINT ["java", "-jar", "/app/server.jar", "5000"]