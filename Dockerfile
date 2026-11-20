# ==============================================================================
# GoldForge API - Runtime Seguro e Otimizado com JRE 21 (Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criação de usuário e grupo sem privilégios de root para segurança (Princípio do Menor Privilégio)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia o artefato compilado (.jar) atribuindo a posse ao usuário não-root
COPY --chown=appuser:appgroup target/*.jar app.jar

# Define o usuário não-root como padrão da execução
USER appuser

# Documenta a porta exposta pela aplicação
EXPOSE 8080

# Flags de JVM otimizadas para containers (reconhece limites de memória do cgroup com até 75% da RAM)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
