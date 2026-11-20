<div align="center">

# ⚒️ GoldForge API

### API RESTful de alta performance para controle financeiro pessoal com motor de gamificação RPG

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Enabled-blue?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/jeps/444)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.1-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)
[![Flyway](https://img.shields.io/badge/Flyway-12%20Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-157%20Passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](#-testes-automatizados)

<br/>

<p align="center">
  <a href="#-destaques-de-engenharia">Destaques</a> •
  <a href="#-motor-de-gamificação-rpg">Gamificação</a> •
  <a href="#-módulos-da-api">Endpoints</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-documentação-swagger">Swagger</a> •
  <a href="#-testes-automatizados">Testes</a> •
  <a href="#-autor">Autor</a>
</p>

---

</div>

## 🎯 Destaques de Engenharia & Arquitetura

O **GoldForge** foi construído sob padrões rigorosos de **Clean Code**, **Domain-Driven Design (DDD)** e arquitetura em camadas estritas (`Controller` -> `Service` -> `Repository`):

* 🛡️ **Segurança em Profundidade (Zero IDOR & OWASP):**
  * **Zero IDOR Mandatório:** Nenhuma rota recebe `userId`. O usuário autenticado é extraído estritamente do contexto de segurança (`SecurityContextHolder` / `@AuthenticationPrincipal`).
  * **Rate Limiting em Camadas:** Janela deslizante exata (`SlidingWindowRateLimiter`) com anti-spoofing de IP (`trust-forwarded-headers`), bloqueando força bruta após 5 falhas.
  * **Exclusão de Dados Híbrida (LGPD):** Soft Delete com carência de 30 dias para usuários (reativação automática no login ou expurgo definitivo via cron com `ON DELETE CASCADE`); **Hard Delete físico definitivo** para transações, contas e orçamentos.
* ⚡ **Performance, I/O & Concorrência:**
  * **Java 21 Virtual Threads (`Project Loom`):** Capacidade para atender milhares de conexões simultâneas com baixo consumo de memória.
  * **Blindagem de Conexões (HikariCP):** Desacoplamento total de chamadas lentas de rede (SMTP) fora de transações de banco de dados, prevenindo exaustão de conexões.
  * **Precisão Financeira com `BigDecimal`:** Valores monetários com escala 2 e arredondamento bancário (`HALF_EVEN`).
  * **Algoritmo de Distribuição de Resíduo (*Remainder Distribution*):** Em compras parceladas no cartão, a diferença de centavos é distribuída matematicamente na primeira parcela.
* 💳 **Domínio Financeiro Moderno:**
  * **Compras Parceladas (`/installments`):** Geração de obrigações futuras em Vencimentos (`Bill`) sem distorcer o fluxo de caixa do mês, com liquidação automática em transações ao quitar.
  * **Metas Financeiras (`/goals`):** Suporte a meta principal, aportes/estornos históricos e cálculos de progresso no servidor.
  * **Orçamentos Flexíveis (`/budgets`):** Tetos por categoria com cálculo de percentual consumido em tempo real.
  * **Categorias Dinâmicas:** Categorias livres normalizadas (`.trim().toUpperCase()`) sem engessamento de enums fixos.

---

## 🎮 Motor de Gamificação RPG

O **GoldForge** transforma a disciplina financeira em uma jornada de RPG:

* **Progressão Suave:** Níveis calculados pela fórmula quadrática $\text{XP}(L) = 50 \times L \times (L - 1)$ (Nv. 1: 0 XP; Nv. 2: 100 XP; Nv. 3: 300 XP...).
* **Psicologia Financeira Positiva (Zero Incentivo Perverso):** O sistema comemora dias sem gastos (`NO_SPEND_DAY`) e aportes em metas (`GOAL_SAVINGS`), recompensando a economia em vez de forçar gastos.
* **Sequências de Check-in (`Streaks`):** Bônus diário de XP para incentivar a disciplina e a consciência financeira.
* **Missões Diárias, Semanais e Marcos:** Resgate de XP com proteção contra resgates duplicados e triggers desacoplados de negócio.

---

## 🧭 Módulos da API

| Módulo | Prefixo | Principais Recursos |
| :--- | :--- | :--- |
| **Autenticação** | `/auth` | Login JWT, ativação por OTP de 6 dígitos, reenvio e recuperação de senha |
| **Usuários & Perfil** | `/users` | Cadastro, dados do perfil (`/users/me`), alteração de senha e exclusão LGPD |
| **Transações** | `/transactions` | CRUD de receitas/despesas, filtros avançados via Specification, ordenação estável |
| **Vencimentos** | `/bills` | Gestão de faturas/boletos, status em tempo real, recorrência e liquidação |
| **Parcelamentos** | `/installments` | Compras parceladas no cartão com cronograma escalonado em Vencimentos |
| **Metas** | `/goals` | Metas de economia, histórico de aportes, promoção de meta principal |
| **Orçamentos** | `/budgets` | Tetos mensais por categoria com avisos de estouro |
| **Gamificação** | `/quests` | Perfil de jogo, missões ativas, check-in diário e resgate de XP |
| **Categorias** | `/categories` | Catálogo sugerido e categorias customizadas dinâmicas do usuário |
| **Notificações** | `/notifications` | Central interna com paginação e preferências (`/users/me/notification-preferences`) |
| **Dashboard** | `/dashboard` | Resumo analítico do período e séries temporais com preenchimento contínuo |
| **Relatórios** | `/reports` | Disparos sob demanda de Relatório Mensal e Alertas Preventivos por e-mail |

---

## 🚀 Como Executar

### Pré-requisitos
* **Docker & Docker Compose** (ou Java 21 LTS + MySQL 8 + Maven)

```bash
# 1. Clone o repositório
git clone https://github.com/isaacggr/GoldForge.git && cd GoldForge

# 2. Configure o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Suba o ambiente 100% conteinerizado (MySQL 8 + API Spring Boot):
docker compose up -d --build
```

> 🌐 A API estará pronta e respondendo em: **`http://localhost:8080`**

---

## 📖 Documentação Swagger / OpenAPI

A API conta com documentação viva e interativa baseada na especificação **OpenAPI 3.1**:

* **Acesse a UI:** **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
* Para autenticar no Swagger: faça login em `POST /auth/login`, copie o token e clique no botão **"Authorize 🔓"**.

---

## 🧪 Testes Automatizados

Suíte robusta de testes unitários e de integração com **JUnit 5**, **Mockito** e **Spring Boot Test**:

```bash
./mvnw test
```

```text
[INFO] Results:
[INFO] Tests run: 157, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (100% verde)
```

---

<div align="center">

Desenvolvido com excelência técnica por **Isaac Gregorio**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/isaacggr)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/isaac-gregorio)

</div>