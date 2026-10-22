FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY config ./config
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/storeops-0.1.0-SNAPSHOT.jar /app/storeops.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/storeops.jar"]
