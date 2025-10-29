# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# 1) POM do parent e POMs dos módulos que o parent referencia
COPY pom.xml ./
COPY config-server/pom.xml config-server/pom.xml
COPY tasks-service/pom.xml tasks-service/pom.xml

# 2) Código-fonte APENAS do tasks-service
COPY tasks-service/src tasks-service/src

# 3) Build apenas do tasks-service (e dependências de módulo)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests -pl tasks-service -am clean package

# 4) Imagem de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app

# usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring

# 5) Copiar EXATAMENTE o *-exec.jar (Boot Jar executável)
COPY --from=build /workspace/tasks-service/target/*-exec.jar app.jar

# 6) Porta do tasks-service
EXPOSE 8081

USER spring
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/opt/app/app.jar"]

