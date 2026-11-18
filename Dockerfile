# Stage 1: build the jar with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# Stage 2: a small image with just a Java runtime and the jar (no Maven, no source code)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home bank
COPY --from=build /app/target/mini-bank.jar app.jar
USER bank
EXPOSE 8080
# Small-server settings: the free hosting tier has 512 MB of RAM and a fraction of a CPU.
# Use up to 75% of RAM for the heap, the lightweight garbage collector, and skip the slowest JIT tier so it starts faster.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
