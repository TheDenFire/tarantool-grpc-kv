FROM maven:3.9.9-eclipse-temurin-17 AS build

LABEL authors="loft"

WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/tarantool-grpc-kv.jar /app/app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
