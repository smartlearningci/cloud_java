# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl tasks-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/tasks-service/target/*.jar /app/app.jar
EXPOSE 8081
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
