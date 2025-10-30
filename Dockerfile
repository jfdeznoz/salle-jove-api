# -------- Stage 1: build (Maven + JDK 21) --------
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /workspace

# Mejor cache de dependencias
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline

# Copiamos el código y compilamos
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests

# Spring Boot por defecto deja el jar en target/*.jar
# Lo dejamos con nombre fijo app.jar para el siguiente stage
RUN JAR_FILE=$(ls target/*.jar | grep -v '\.original$' | head -n 1) && \
    cp "$JAR_FILE" /workspace/app.jar

# -------- Stage 2: runtime (JRE 21, no-root) --------
FROM amazoncorretto:21-alpine AS runtime
# Crea usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

# Ajustes típicos para Spring Boot en contenedor
ENV SERVER_PORT=5000 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 5000
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT} --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]