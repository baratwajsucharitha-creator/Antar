# Multi-stage: the build JDK never ships.
# Local parity and portability; the Azure deploy uses the JAR on App Service Java SE,
# because container hosting requires a paid tier and the free tier runs the JAR fine.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependency layer first, so a source change does not re-download the world.
COPY pom.xml .
COPY antar-engine/pom.xml antar-engine/
COPY antar-api/pom.xml antar-api/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY antar-engine/src antar-engine/src
COPY antar-api/src antar-api/src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Never run as root.
RUN addgroup -S antar && adduser -S antar -G antar
USER antar

COPY --from=build /build/antar-api/target/antar-api-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
