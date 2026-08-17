# ============================================================
# Build em DOIS ESTAGIOS.
#
# Estagio 1 (build): tem o JDK e o Maven, compila o projeto.
# Estagio 2 (run):   tem so o JRE e o .jar pronto.
#
# Por que separar: a imagem final NAO carrega compilador, codigo
# fonte nem dependencias de build. Fica pequena (~200MB em vez
# de ~800MB) e com menos superficie de ataque.
# ============================================================

# ---------- estagio 1: compilar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -B

# ---------- estagio 2: rodar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# leva SO o jar do estagio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# -XX:MaxRAMPercentage=75 -> a JVM usa ate 75% da RAM do container.
#   Sem isso ela chuta errado em container pequeno e leva OutOfMemory.
# -XX:+UseSerialGC -> coletor simples, melhor com pouca RAM/CPU.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-jar", "app.jar"]
