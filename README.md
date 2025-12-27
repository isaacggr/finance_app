# Finance App 💰

**Gerenciador de finanças pessoais** simples construído com Spring Boot.

---

## 🔍 Visão geral

Aplicação backend que permite gerenciar usuários e transações financeiras (receitas e despesas). Fornece endpoints REST para criar/consultar/deletar usuários e transações, com validações básicas e soft delete.

## ⚙️ Tecnologias

-   Java 21
-   Spring Boot 4 (WebMVC, Data JPA, Validation, Security)
-   H2 (console) / MariaDB (produção)
-   JPA + Hibernate
-   Lombok (construtores/getters)

## ✅ Funcionalidades principais

-   CRUD (parcial) de Usuários (criar, buscar por id, soft delete)
-   CRUD (parcial) de Transações (criar, buscar por id, listar por usuário com paginação, soft delete)
-   Validações de entrada (Bean Validation)
-   Tratamento centralizado de exceções com respostas padronizadas
-   Senha armazenada com hashing (BCrypt)
-   H2 console disponível para desenvolvimento (/h2-console)

## 📁 Estrutura principal do projeto

-   `com.isaacggr.finance_app.controller` - endpoints REST
-   `domain.entity` - entidades JPA (`User`, `Transaction`)
-   `dto` - objetos de transferência (request/response)
-   `repository` - interfaces JPA
-   `service` - regras de negócio
-   `exception` - exceções e handler global
-   `config` - configurações (Security, Beans)

---

## 🚀 Como executar localmente

Pré-requisitos: Java 21 e Maven.

1. Copie `src/main/resources/application-example.yaml` para `application.yaml` e ajuste as credenciais do banco (ou use H2 em memória para dev).

2. Rodar com o wrapper Maven:

```bash
./mvnw spring-boot:run
```

Ou empacotar e executar:

```bash
./mvnw clean package
java -jar target/finance-app-0.0.1-SNAPSHOT.jar
```

3. Acesse a aplicação em: `http://localhost:8080`

> Para ver o console do H2 (ambiente dev): `http://localhost:8080/h2-console`

---

## 🧭 Endpoints principais

Base: `http://localhost:8080`

-   Usuários

    -   `POST /users` — Criar usuário
        ```json
        {
            "name": "João Silva",
            "email": "joao@example.com",
            "password": "senhaSegura"
        }
        ```
    -   `GET /users/{id}` — Buscar usuário por id
    -   `DELETE /users/{id}` — Soft delete

-   Transações
    -   `POST /transactions` — Criar transação
        ```json
        {
          "userId": "<uuid-do-usuario>",
          "amount": 150.50,
          "type": "INCOME",        # INCOME | EXPENSE
          "category": "ALIMENTACAO", # ver lista abaixo
          "date": "2025-12-27",
          "description": "Salário / compra"
        }
        ```
    -   `GET /transactions/{transactionId}` — Buscar transação por id
    -   `GET /transactions/user/{userId}` — Listar transações do usuário (paginação)
    -   `DELETE /transactions/{transactionId}` — Soft delete

Categorias disponíveis (enum `TransactionCategory`):
`ALIMENTACAO`, `ALUGUEL`, `TRANSPORTE`, `LAZER`, `CARTÃO`, `INVESTIMENTOS`, `EDUCAÇÃO`, `OUTROS`.

---

## 🧪 Testes

Rodar:

```bash
./mvnw test
```

---

## 🛡️ Tratamento de erros

Erros retornam JSON com a estrutura:

```json
{
    "status": 404,
    "message": "Usuário não encontrado",
    "timestamp": "2025-12-27T12:00:00"
}
```

Códigos usados: 400 (bad request), 404 (not found), 409 (conflict), 500 (erro interno).

---

## 💡 Observações & próximos passos sugeridos

-   Adicionar autenticação JWT para proteger endpoints
-   Documentar API com OpenAPI / Swagger
-   Adicionar testes de integração e CI
-   Scripts de migração (Flyway/Liquibase)

---

## 🤝 Contribuição

Contribuições são bem-vindas! Abra uma issue ou envie um PR.

---

## 📄 Licença ✅

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

Este projeto está licenciado sob a **Licença MIT** — consulte o arquivo `LICENSE` para os termos completos.

© 2025 Isaac Gregorio
