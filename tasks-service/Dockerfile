# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY discovery/pom.xml discovery/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY tasks-service/pom.xml tasks-service/pom.xml

COPY tasks-service/src tasks-service/src

RUN  mvn -q -DskipTests -pl tasks-service -am clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/tasks-service/target/*-exec.jar app.jar
EXPOSE 8080
USER spring
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/opt/app/app.jar"]

