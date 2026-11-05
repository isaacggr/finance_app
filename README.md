<div align="center">

# 💰 Finance App

### API RESTful moderna para controle e gestão financeira pessoal

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.1-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui.html)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-65%20Passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](#-testes-automatizados)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](./LICENSE)

<br/>

<p align="center">
  <a href="#-sobre-a-aplicação">Sobre</a> •
  <a href="#-arquitetura-em-camadas">Arquitetura</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-documentação-interativa-swagger">Swagger</a> •
  <a href="#-endpoints-da-api">Endpoints</a> •
  <a href="#-testes-automatizados">Testes</a> •
  <a href="#-autor">Autor</a>
</p>

---

</div>

## 🎯 Sobre a Aplicação

O **Finance App** foi desenvolvido para simplificar o controle financeiro pessoal através de uma API robusta, escalável e segura.

### ✨ Principais Recursos e Decisões de Engenharia:

* **🔐 Autenticação Stateless (JWT) & Defesa em Profundidade:** Sessões stateless gerenciadas via tokens JWT assinados com algoritmo **HMAC-SHA256**. O `SecurityFilter` realiza dupla validação: confere a assinatura criptográfica e garante que a conta do usuário permanece ativa e com e-mail verificado.
* **✉️ Verificação de E-mail Scoped por Usuário:** Confirmação de conta através de código numérico de 6 dígitos enviado por e-mail (`email` + `code`), eliminando riscos de colisão global no banco e garantindo bloqueio estrito de login para contas não confirmadas (HTTP 403 Forbidden).
* **🛡️ Proteção contra Força Bruta & Lockout (OWASP):** Limite estrito de 5 tentativas consecutivas para inserção de códigos de ativação e redefinição de senha. Atingido o limite, o token é imediatamente invalidado por segurança.
* **🔑 Recuperação Segura de Senha (OWASP):** Redefinição de senha com códigos temporários de 6 dígitos via e-mail e proteção contra enumeração de usuários (retorno neutro para evitar vazamento de existência de contas).
* **🌐 Suporte Integrado a CORS:** Configuração de `CorsConfigurationSource` habilitada no Spring Security com suporte a credenciais e múltiplos métodos HTTP para conexão imediata com SPAs e frontends (React, Vue, Vite, Next.js).
* **🛡️ Eliminação Estrita de IDOR (OWASP):** As rotas de transações **nunca** recebem `userId` no corpo ou na URL. O usuário autenticado é extraído diretamente do contexto de segurança (`SecurityContextHolder`), garantindo isolamento total de dados entre contas.
* **⚡ Alta Concorrência com Virtual Threads:** Habilitado o modelo leve do Java 21 (`spring.threads.virtual.enabled=true`), permitindo atender milhares de conexões I/O simultâneas sem sobrecarregar a memória do sistema operacional.
* **👤 Gestão de Usuários:** Cadastro com validação de formato e senhas criptografadas com **BCrypt**.
* **💳 Gestão de Transações:** CRUD completo (criação, consulta, atualização e remoção) com receitas (`INCOME`) e despesas (`EXPENSE`) organizadas por categorias.
* **🔍 Filtros Financeiros Dinâmicos:** Consulta ao extrato com filtros opcionais combinados via **JPA Specification** por período (`startDate`/`endDate`), categoria e tipo.
* **📊 Dashboard & Métricas de Alta Performance:** Resumo analítico com total de receitas, despesas, saldo líquido e detalhamento por categoria calculado via **agregação nativa SQL (`SUM`, `GROUP BY`)** no banco de dados.
* **🎯 Metas e Orçamentos Mensais (Budgets):** Definição de limites de gastos por categoria e mês/ano, com monitoramento em tempo real do progresso (% consumido, saldo restante e alerta de estouro de teto).
* **⏰ Alertas Preventivos de Contas a Vencer:** Rotina agendada diária (`@Scheduled` às 09:00 AM) que rastreia despesas pendentes nos próximos dias (janela padrão de 3 dias) e envia notificações por e-mail com aviso visual amigável, além de endpoint sob demanda (`POST /reports/bill-alerts/send-me`).
* **📬 Relatórios Mensais Automatizados:** Rotina periódica agendada (`@Scheduled`) que consolida o extrato financeiro do mês fechado e despacha um e-mail com template HTML corporativo, além de endpoint sob demanda (`POST /reports/monthly/send-me`).
* **🐳 Conteinerização Multi-Stage (Docker):** Imagem de produção mínima baseada em Alpine Linux (~180MB) executando com usuário seguro não-root (Princípio do Menor Privilégio) e orquestração sincronizada via Docker Compose com healthcheck no MySQL.
* **📄 Paginação Limpa e Otimizada:** Extrato financeiro paginado utilizando um DTO padronizado (`PageResponseDTO`), eliminando metadados internos redundantes da resposta.
* **📦 Versionamento de Banco com Flyway:** Schema SQL versionado de forma declarativa e auditável (`V1__create_initial_schema.sql`), desativando o modo inseguro de autogeração do Hibernate (`ddl-auto: validate`).
* **🗑️ Exclusão Lógica (*Soft Delete*):** Registros nunca são deletados fisicamente do banco de dados, preservando a integridade histórica.
* **⏱️ Auditoria Automática:** Preenchimento automático de data/hora de criação (`created_at`) e atualização (`updated_at`) via ciclo de vida JPA (`@PrePersist` e `@PreUpdate`).

---

## 🏛️ Arquitetura em Camadas

O projeto segue os princípios de **Clean Code**, **SOLID** e **Arquitetura em Camadas (Layered Architecture)**:

```text
               ┌────────────────────────────────────────────────────────┐
               │                  Cliente / Requisição                  │
               └───────────────────────────┬────────────────────────────┘
                                           │ HTTP / JSON (Bearer Token)
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  1. Security & Filter Layer                                                            │
│  • Validação de Token JWT (OncePerRequestFilter) e injeção no SecurityContextHolder    │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  2. Controller Layer                                                                   │
│  • Mapeamento REST, validação Bean Validation (@Valid) e injeção do @AuthenticationPrincipal │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  3. Service Layer                                                                      │
│  • 100% das regras de negócio, controle transacional (@Transactional) e regras de domínio│
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  4. Repository Layer                                                                   │
│  • Spring Data JPA com consultas filtradas por usuário autenticado e soft delete       │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  5. Database Layer (Docker)                                                            │
│  • MySQL 8.0 conteinerizado, desacoplado via .env e persistido em volume local          │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Como Executar

### Pré-requisitos
* **Java 21**
* **Docker & Docker Compose**

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/isaacggr/finance_app.git
cd finance_app

# 2. Configure o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Execução 100% Conteinerizada (MySQL + API Spring Boot):
docker compose up -d --build

# OU se preferir rodar a API localmente via Maven:
docker compose up -d finance-mysql
./mvnw spring-boot:run
```

> 🌐 A API estará disponível em: **`http://localhost:8080`**

---

## 📖 Documentação Interativa (Swagger)

A API possui documentação viva e interativa baseada na especificação **OpenAPI 3.1**:

* Acesse a interface Swagger UI: **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
* Para testar rotas protegidas: faça login em `POST /auth/login`, copie o token e use o botão **"Authorize 🔓"**.

---

## 🧭 Endpoints da API

### 🔐 Autenticação (`/auth`)

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `POST` | `/auth/login` | Não | Autentica o usuário com e-mail/senha e retorna o token JWT Bearer (exige conta ativa) |
| `POST` | `/auth/verify-email` | Não | Confirma a ativação da conta via JSON (`email` + `code` de 6 dígitos) com proteção anti-força bruta |
| `POST` | `/auth/resend-code` | Não | Reenvia o código de ativação caso o anterior tenha expirado ou sido perdido |
| `POST` | `/auth/forgot-password` | Não | Solicita código de 6 dígitos para recuperação de senha (proteção anti-enumeração) |
| `POST` | `/auth/reset-password` | Não | Redefine a senha utilizando o código de 6 dígitos e a nova senha |

---

### 👤 Usuários (`/users`)

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `POST` | `/users` | Não | Cadastra um novo usuário com senha criptografada via BCrypt |
| `GET` | `/users/me` | 🔒 Sim | Retorna os dados do próprio usuário autenticado (Zero IDOR) |
| `DELETE` | `/users/me` | 🔒 Sim | Realiza a exclusão lógica (*soft delete*) da conta do próprio usuário |

---

### 💳 Transações (`/transactions`)

> 🔒 *Todas as operações de transação são estritamente associadas ao usuário autenticado.*

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `POST` | `/transactions` | 🔒 Sim | Cria uma receita (`INCOME`) ou despesa (`EXPENSE`) para o usuário autenticado |
| `GET` | `/transactions` | 🔒 Sim | Lista as transações do usuário com paginação e filtros opcionais por data, categoria e tipo |
| `GET` | `/transactions/{id}` | 🔒 Sim | Busca uma transação por ID (retorna 404 se pertencer a outro usuário) |
| `PUT` | `/transactions/{id}` | 🔒 Sim | Atualiza os dados de uma transação pertencente ao usuário autenticado |
| `DELETE` | `/transactions/{id}` | 🔒 Sim | Exclui logicamente a transação do usuário autenticado |

---

### 📊 Dashboard (`/dashboard`)

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `GET` | `/dashboard/summary` | 🔒 Sim | Retorna o resumo financeiro consolidado (receitas, despesas, saldo e gastos por categoria) |

---

### 🎯 Orçamentos (`/budgets`)

> 🔒 *Todas as operações de orçamento são estritamente associadas ao usuário autenticado.*

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `POST` | `/budgets` | 🔒 Sim | Define uma meta/teto de gastos para uma categoria em determinado mês/ano |
| `GET` | `/budgets` | 🔒 Sim | Lista as metas cadastradas pelo usuário para o mês e ano informados |
| `GET` | `/budgets/progress` | 🔒 Sim | Retorna o progresso em tempo real (gasto, restante, % consumido e alerta de estouro) |
| `DELETE` | `/budgets/{id}` | 🔒 Sim | Exclui logicamente (*soft delete*) o orçamento do usuário autenticado |

---

### 📑 Relatórios & Inteligência Financeira (`/reports`)

> 🔒 *Disparo de extratos consolidados e alertas preditivos por e-mail para o usuário autenticado.*

| Verbo | Rota | Autenticado? | Descrição |
| :---: | :--- | :---: | :--- |
| `POST` | `/reports/monthly/send-me` | 🔒 Sim | Envia o relatório mensal com receitas, despesas, saldo e categorias para o e-mail do usuário |
| `POST` | `/reports/bill-alerts/send-me` | 🔒 Sim | Envia alerta de contas com vencimento nos próximos dias (`daysAhead`, padrão: 3) |

---

## 🧪 Testes Automatizados

A aplicação conta com **65 testes unitários e de integração** com **JUnit 5** e **Mockito**, cobrindo regras de negócio, segurança e automações:

```bash
./mvnw test
```

```text
[INFO] Running com.isaacggr.finance_app.service.UserServiceTest (8 tests)
[INFO] Running com.isaacggr.finance_app.service.VerificationTokenServiceTest (9 tests)
[INFO] Running com.isaacggr.finance_app.service.BillAlertServiceTest (4 tests)
[INFO] Running com.isaacggr.finance_app.service.DashboardServiceTest (3 tests)
[INFO] Running com.isaacggr.finance_app.service.TransactionServiceTest (14 tests)
[INFO] Running com.isaacggr.finance_app.service.AuthServiceTest (10 tests)
[INFO] Running com.isaacggr.finance_app.service.MonthlyReportServiceTest (5 tests)
[INFO] Running com.isaacggr.finance_app.service.TokenServiceTest (4 tests)
[INFO] Running com.isaacggr.finance_app.service.BudgetServiceTest (7 tests)
[INFO] Running com.isaacggr.finance_app.FinaceAppApplicationTests (1 test)
[INFO] 
[INFO] Results:
[INFO] Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

<div align="center">

## 👤 Autor

Desenvolvido por **Isaac Gregorio**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/isaacggr)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/isaac-gregorio)

</div>
