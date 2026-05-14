FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

RUN mv build/libs/*[!plain].jar build/libs/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
EXPOSE 8080

COPY --from=build /home/gradle/src/build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]