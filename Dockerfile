# One Maven command compiles/tests Java and installs/builds Angular into the JAR.
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B verify

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system lumina && useradd --system --gid lumina --home-dir /app lumina
COPY --from=build --chown=lumina:lumina /workspace/target/lumina-clinic-1.0.0.jar /app/app.jar
USER lumina:lumina
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=20.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
