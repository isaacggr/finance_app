<div align="center">

# 💰 Finance App

### API RESTful de alta performance para controle e inteligência financeira pessoal

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.1-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-66%20Passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](#-testes-automatizados)

<br/>

<p align="center">
  <a href="#-destaques-de-engenharia">Destaques</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-documentação-swagger">Swagger</a> •
  <a href="#-módulos-da-api">Endpoints</a> •
  <a href="#-testes-automatizados">Testes</a> •
  <a href="#-autor">Autor</a>
</p>

---

</div>

## 🎯 Destaques de Engenharia

Construída sob princípios de **Clean Code**, **Domain-Driven Design (DDD)** e arquitetura em camadas estritas (`Controller` -> `Service` -> `Repository`):

* 🛡️ **Segurança em Profundidade (OWASP):**
  * **Zero IDOR:** O usuário autenticado é extraído estritamente do token JWT (`@AuthenticationPrincipal`).
  * **Proteção Anti-Brute Force:** Bloqueio e invalidação de código de verificação após 5 tentativas consecutivas.
  * **Anti-Enumeração & Lockout:** Recuperação de senha e ativação por e-mail com respostas neutras e uniformes.
  * **Suporte Completo a CORS:** Configuração pronta para integração com SPAs (React, Vue, Vite).
* ⚡ **Performance & Concorrência:**
  * **Java 21 Virtual Threads:** Capacidade para milhares de conexões I/O simultâneas sem bloqueio de threads de SO.
  * **Agregação SQL Nativa:** Totais do Dashboard e gastos por categoria calculados via `SUM` e `GROUP BY` no MySQL.
* 💳 **Domínio Financeiro Flexível:**
  * **Categorias Livres:** Categorias abertas (ex: `ALIMENTACAO`, `PETS`, `FARMACIA`), normalizadas e sanitizadas automaticamente em caixa alta pela API.
  * **Exclusão Híbrida:** Soft Delete para contas de usuário e **Hard Delete físico definitivo** para transações (Direito ao Esquecimento / LGPD).
  * **Metas & Orçamentos (Budgets):** Monitoramento de tetos de gastos com cálculo percentual consumido em tempo real.
* 📬 **Inteligência Ativa & Automações (`@Scheduled`):**
  * **Alertas de Contas a Vencer:** Rotina diária (09:00 AM) que notifica despesas pendentes dos próximos 3 dias por e-mail.
  * **Relatório Mensal Consolidado:** Fechamento mensal com balanço e gráfico de despesas despachado por e-mail.

---

## 🚀 Como Executar

### Pré-requisitos
* **Docker & Docker Compose** (ou Java 21 LTS + Maven)

```bash
# 1. Clone o repositório
git clone https://github.com/isaacggr/finance_app.git && cd finance_app

# 2. Configure o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Suba o ambiente 100% conteinerizado (MySQL 8 + API Spring Boot):
docker compose up -d --build
```

> 🌐 A API estará pronta em: **`http://localhost:8080`**

---

## 📖 Documentação Swagger

A API conta com documentação viva e interativa baseada na especificação **OpenAPI 3.1**:

* **Acesse a UI:** **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
* Para autenticar no Swagger: faça login em `POST /auth/login`, copie o token e clique no botão **"Authorize 🔓"**.

---

## 🧭 Módulos da API

| Módulo | Prefixo | Principais Recursos |
| :--- | :--- | :--- |
| **Autenticação** | `/auth` | Login JWT, ativação por código (6 dígitos), reenvio e redefinição de senha com anti-brute force |
| **Usuários** | `/users` | Cadastro público, perfil autenticado (`/users/me`) e soft delete de conta |
| **Transações** | `/transactions` | CRUD de receitas e despesas com categoria aberta normalizada, paginação e Hard Delete |
| **Dashboard** | `/dashboard` | Resumo analítico consolidado (receitas, despesas, saldo e maiores categorias) |
| **Orçamentos** | `/budgets` | Definição de teto de gastos por categoria e mês com cálculo de progresso e estouro |
| **Relatórios** | `/reports` | Disparo sob demanda de Relatório Mensal e Alerta de Contas a Vencer para o e-mail |

---

## 🧪 Testes Automatizados

Suíte completa com **JUnit 5** e **Mockito**, validando regras de negócio, limites de segurança e cálculos analíticos:

```bash
./mvnw test
```

```text
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (100% verde)
```

---

<div align="center">

Desenvolvido por **Isaac Gregorio**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/isaacggr)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/isaac-gregorio)

</div>
