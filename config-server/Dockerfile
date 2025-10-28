# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY config-server/pom.xml config-server/pom.xml
COPY tasks-service/pom.xml tasks-service/pom.xml

COPY config-server/src config-server/src

RUN  mvn -q -DskipTests -pl config-server -am clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/config-server/target/*-exec.jar app.jar
EXPOSE 8888
USER spring
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/opt/app/app.jar"]
