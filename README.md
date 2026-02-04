# salle-jove-api

API backend para la gestión de eventos y usuarios de Salle Joven. Construida con Spring Boot 3.4.1.

## Requisitos Previos

- **Java 17** o superior (recomendado Java 17)
- **Maven 3.6+**
- **PostgreSQL** 12+ (en ejecución)
- **Certificados RSA** para JWT (archivos `.pem`)

## Configuración Inicial

### 1. Base de Datos PostgreSQL

Asegúrate de tener PostgreSQL ejecutándose localmente con las siguientes credenciales (o actualiza `application-local.yml`):

- **Host**: `localhost`
- **Puerto**: `5432`
- **Base de datos**: `sallejoven`
- **Usuario**: `postgres`
- **Contraseña**: `root`

Puedes crear la base de datos con:

```sql
CREATE DATABASE sallejoven;
```

### 2. Certificados RSA para JWT

La aplicación requiere certificados RSA para firmar y validar tokens JWT. Asegúrate de tener los siguientes archivos en `src/main/resources/certs/`:

- `privateKey.pem`
- `publicKey.pem`

Si no los tienes, puedes generarlos con OpenSSL:

```bash
# Generar clave privada
openssl genpkey -algorithm RSA -out src/main/resources/certs/privateKey.pem -pkeyopt rsa_keygen_bits:2048

# Generar clave pública
openssl rsa -pubout -in src/main/resources/certs/privateKey.pem -out src/main/resources/certs/publicKey.pem
```

## Ejecutar la Aplicación

### Opción 1: Usando Maven Wrapper (Recomendado)

```bash
cd salle-jove-api

# Ejecutar en modo local
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# O en Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

### Opción 2: Compilar y Ejecutar JAR

```bash
# Compilar el proyecto
./mvnw clean package -DskipTests

# Ejecutar el JAR generado
java -jar target/salle-joven-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Opción 3: Usando Maven directamente

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Opción 4: Ejecutar desde el IDE

1. Abre el proyecto en tu IDE (IntelliJ IDEA, Eclipse, etc.)
2. Ubica la clase principal: `com.sallejoven.backend.SalleJovenApplication`
3. Ejecuta la clase con el perfil activo `local`
4. En IntelliJ: Run → Edit Configurations → Active profiles: `local`

## Configuración de Variables de Entorno (Opcional)

Si prefieres usar variables de entorno en lugar de modificar `application-local.yml`:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/sallejoven
export DB_USER=postgres
export DB_PASSWORD=root
export PORT=8081
export SPRING_PROFILES_ACTIVE=local
```

Luego ejecuta la aplicación normalmente.

## Puertos

- **Puerto por defecto (local)**: `8081`
- El puerto puede configurarse mediante la variable de entorno `PORT` o en `application.yml`

## Endpoints Disponibles

Una vez que la aplicación esté ejecutándose, podrás acceder a:

- **API Base**: `http://localhost:8081`
- **Health Check**: `http://localhost:8081/actuator/health` (si está habilitado)

## Migraciones de Base de Datos

La aplicación usa **Liquibase** para gestionar migraciones de base de datos. Las migraciones se ejecutan automáticamente al iniciar la aplicación.

Los archivos de migración se encuentran en: `src/main/resources/db/changelog/changes/`

## Perfiles Disponibles

- **local**: Desarrollo local (usa `application-local.yml`)
- **prod**: Producción (usa `application-prod.yml`)

## Solución de Problemas

### Error de conexión a la base de datos

Verifica que PostgreSQL esté ejecutándose:
```bash
# En macOS/Linux
brew services list  # o systemctl status postgresql

# Verifica la conexión
psql -h localhost -U postgres -d sallejoven
```

### Error de certificados RSA faltantes

Asegúrate de que los archivos `privateKey.pem` y `publicKey.pem` existan en `src/main/resources/certs/`

### Puerto ya en uso

Si el puerto 8081 está ocupado, cambia el puerto en `application.yml` o usa:
```bash
java -jar target/salle-joven-0.0.1-SNAPSHOT.jar --server.port=8082
```

## Ejecutar con Docker

Para ejecutar la API usando Docker:

```bash
# Construir la imagen
docker build -t salle-joven-api .

# Ejecutar el contenedor
docker run -p 5000:5000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/sallejoven \
  -e DB_USER=postgres \
  -e DB_PASSWORD=root \
  salle-joven-api
```

## Estructura del Proyecto

```
salle-jove-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/sallejoven/backend/
│   │   │       ├── config/        # Configuraciones (Security, S3, etc.)
│   │   │       ├── controller/    # Controladores REST
│   │   │       ├── model/         # Entidades y DTOs
│   │   │       ├── repository/    # Repositorios JPA
│   │   │       ├── service/       # Lógica de negocio
│   │   │       └── errors/        # Manejo de errores
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       ├── certs/             # Certificados RSA
│   │       └── db/                # Migraciones Liquibase
│   └── test/
├── pom.xml
└── README.md
```

## Tecnologías Utilizadas

- Spring Boot 3.4.1
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL
- Liquibase
- AWS SDK (S3, SQS)
- Lombok
- Maven
