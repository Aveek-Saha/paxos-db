# Build stage
FROM maven:3.9.6-eclipse-temurin-11-alpine AS build
COPY . /app
WORKDIR /app/client
RUN mvn clean install package

# Run stage
FROM amazoncorretto:11-alpine3.19 AS client-build
COPY --from=build /app/client/target/client-4.0-jar-with-dependencies.jar /app/client.jar