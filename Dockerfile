# Stage 1: Build the application
FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/src

# Copy only the dependency configuration files first to leverage Docker caching
COPY build.gradle settings.gradle /home/gradle/src/

# Copy the actual source code
COPY --chown=gradle:gradle . /home/gradle/src

# Force a clean build to destroy any local, cached non-GraphQL JARs
RUN gradle clean build --no-daemon -x test

# Rename the fat JAR to a standardized name, ignoring the 'plain' JAR
RUN mv build/libs/*[!plain].jar build/libs/app.jar

# Stage 2: Minimal Runtime Environment
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Expose the default Spring Boot port
EXPOSE 8080

# Copy the freshly compiled JAR from Stage 1
COPY --from=build /home/gradle/src/build/libs/app.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
