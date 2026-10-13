FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY producto-service/pom.xml producto-service/pom.xml
COPY pedido-service/pom.xml pedido-service/pom.xml
RUN mvn -B -pl producto-service -am dependency:go-offline

COPY producto-service/src producto-service/src
RUN mvn -B -pl producto-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
COPY --from=build /workspace/producto-service/target/producto-service-2.0.0.jar app.jar
USER spring

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
