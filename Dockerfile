# ==============================================================================
# ESTÁGIO 1: BUILDER (Compilação e empacotamento com JDK 21)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copia arquivos de configuração do Maven e o wrapper primeiro para aproveitar o cache de camadas do Docker
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução no wrapper
RUN chmod +x mvnw

# Baixa as dependências offline (camada de cache reaproveitada caso o pom.xml não mude)
RUN ./mvnw dependency:go-offline -B

# Copia o código-fonte da aplicação
COPY src/ src/

# Compila e empacota o executável .jar (testes executados separadamente na pipeline de CI)
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# ESTÁGIO 2: RUNTIME (Imagem mínima e segura de produção com JRE 21)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criação de usuário e grupo sem privilégios de root para segurança (Princípio do Menor Privilégio)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia apenas o .jar gerado no estágio anterior, atribuindo a posse ao usuário não-root
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Define o usuário não-root como padrão da execução
USER appuser

# Documenta a porta exposta pela aplicação
EXPOSE 8080

# Flags de JVM otimizadas para containers (reconhece limites de memória do cgroup com até 75% da RAM)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

