FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn -q verify

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Run as a non-root user; Kubernetes also enforces runAsNonRoot.
RUN groupadd --system --gid 10001 lottofun && useradd --system --uid 10001 --gid lottofun lottofun
COPY --from=builder /app/target/*.jar app.jar
USER 10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
