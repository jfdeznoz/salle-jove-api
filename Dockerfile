# Usar una imagen base de OpenJDK
FROM openjdk:11-jre-slim

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo JAR de la aplicación al contenedor
COPY target/salle-joven-0.0.1-SNAPSHOT.jar /app/salle-joven.jar

# Exponer el puerto en el que la aplicación va a correr
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "salle-joven.jar"]