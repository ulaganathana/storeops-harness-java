FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY config config
COPY src src
RUN mvn -B -q -DskipTests -Dcheckstyle.skip package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/storeops-api-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
