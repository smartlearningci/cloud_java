# syntax=docker/dockerfile:1.6

# ===== build stage =====
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Copiar POMs para resolver dependências
COPY pom.xml ./
COPY discovery/pom.xml discovery/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY tasks-service/pom.xml tasks-service/pom.xml

# Copiar código-fonte do módulo alvo
COPY discovery/src discovery/src

# Compilar o módulo alvo
RUN  mvn -q -DskipTests -pl discovery -am clean package

# ===== runtime stage =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/discovery/target/*-exec.jar app.jar
EXPOSE 8761
USER spring
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/opt/app/app.jar"]
