# --- Build stage: compile with Maven and JDK 21 ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM first and pre-fetch dependencies (improves incremental build speed)
COPY pom.xml .

# Copy sources and build the fat jar
COPY src ./src
RUN mvn -q -DskipTests clean package 

# --- Runtime stage: slim JRE 21 only ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy executable jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the HTTP port
EXPOSE 8081

# If you need remote debug, run container with:
#   -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
#   -p 5005:5005
ENTRYPOINT ["java","-jar","/app/app.jar"]

