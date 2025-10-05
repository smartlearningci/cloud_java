# Build stage: compile and package the executable JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests clean package

# Runtime stage: slim Java 21 JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
