# Dockerfile para desplegar BackEnd_ContrastIQ en Render.
# Build multi-stage: la primera etapa compila con Maven + JDK 17, la
# segunda solo copia el .jar ya compilado a una imagen liviana con JRE 17
# (no arrastra Maven ni el codigo fuente a la imagen final que corre).

# --- Etapa 1: compilar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copiar primero solo el pom.xml y descargar dependencias -- Docker cachea
# esta capa, asi que en rebuilds futuros (si solo cambia el codigo Java)
# no vuelve a descargar todo Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline
# Ahora si copiar el codigo fuente completo y compilar el jar.
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etapa 2: imagen de ejecucion ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# El nombre del jar sale de pom.xml: artifactId=backend-contrastiq, version=1.0.0
COPY --from=build /app/target/backend-contrastiq-1.0.0.jar app.jar

# Render inyecta la variable de entorno PORT en tiempo de ejecucion (ver
# server.port=${PORT:8080} en application.properties) -- este EXPOSE es
# solo documentacion, Render no depende de el para enrutar.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
