# -------- Stage 1: build (Maven + JDK 21) --------
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests
RUN JAR_FILE=$(ls target/*.jar | grep -v '\.original$' | head -n 1) && \
    cp "$JAR_FILE" /workspace/app.jar

# -------- Stage 2: runtime (JRE 21, no-root) --------
FROM amazoncorretto:21-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

# Una sola variable de puerto
ENV PORT=5000 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 5000

# No forzamos --server.port por CLI; dejamos que application.yml lea ${PORT}
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
