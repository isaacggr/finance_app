<div align="center">

# 💰 Finance App

### API RESTful moderna para controle e gestão financeira pessoal

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-17%20Passed-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](#-testes-automatizados)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](./LICENSE)

<br/>

<p align="center">
  <a href="#-sobre-a-aplicação">Sobre</a> •
  <a href="#-arquitetura-em-camadas">Arquitetura</a> •
  <a href="#-como-executar">Como Executar</a> •
  <a href="#-endpoints-da-api">Endpoints</a> •
  <a href="#-testes-automatizados">Testes</a> •
  <a href="#-autor">Autor</a>
</p>

---

</div>

## 🎯 Sobre a Aplicação

O **Finance App** foi desenvolvido para simplificar o controle financeiro pessoal através de uma API simples, rápida e segura.

### ✨ Como a aplicação funciona:

* **👤 Gestão de Usuários:** Cadastro completo com validação de dados e senha criptografada com **BCrypt**.
* **💳 Gestão de Transações:** Registro de receitas (`INCOME`) e despesas (`EXPENSE`) organizadas por categorias (alimentação, moradia, transporte, lazer, etc.).
* **📄 Extrato Paginado:** Consulta otimizada das transações financeiras de um usuário, com suporte a paginação e ordenação por data.
* **🛡️ Exclusão Lógica (*Soft Delete*):** Usuários e transações não são excluídos fisicamente do banco de dados; uma flag de inativação preserva a integridade do histórico financeiro.
* **⏱️ Auditoria Automática:** Todas as entidades registram automaticamente carimbos de data/hora de criação (`created_at`) e última atualização (`updated_at`).

---

## 🏛️ Arquitetura em Camadas

O projeto foi estruturado seguindo o padrão de **Arquitetura em Camadas (Layered Architecture)**, garantindo separação clara de responsabilidades, testabilidade e manutenibilidade:

```text
               ┌────────────────────────────────────────────────────────┐
               │                  Cliente / Requisição                  │
               └───────────────────────────┬────────────────────────────┘
                                           │ HTTP / JSON
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  1. Controller Layer                                                                   │
│  • Mapeamento de rotas REST e validação de contratos de entrada via DTOs (Records)     │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  2. Service Layer                                                                      │
│  • Regras de negócio, validações financeiras, segurança (BCrypt) e controle de estado │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  3. Repository Layer                                                                   │
│  • Abstração de persistência com Spring Data JPA, paginação e consultas de soft delete │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  4. Database Layer (Docker)                                                            │
│  • Banco de dados relacional MySQL 8.0 conteinerizado com persistência em volume       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Como Executar

### Pré-requisitos
* **Java 21**
* **Docker & Docker Compose**

```bash
# 1. Inicie o banco de dados MySQL no Docker
docker compose up -d

# 2. Inicie a aplicação Spring Boot
./mvnw spring-boot:run
```

> 🌐 A API estará disponível em: **`http://localhost:8080`**

---

## 🧭 Endpoints da API

### 👤 Usuários (`/users`)

| Verbo | Rota | Descrição |
| :---: | :--- | :--- |
| `POST` | `/users` | Cadastra um novo usuário com senha criptografada |
| `GET` | `/users/{id}` | Busca os dados do usuário por ID |
| `DELETE` | `/users/{id}` | Realiza a exclusão lógica (*soft delete*) do usuário |

---

### 💳 Transações (`/transactions`)

| Verbo | Rota | Descrição |
| :---: | :--- | :--- |
| `POST` | `/transactions` | Cria uma nova receita (`INCOME`) ou despesa (`EXPENSE`) |
| `GET` | `/transactions/{id}` | Busca os detalhes de uma transação por ID |
| `GET` | `/transactions/user/{userId}` | Lista transações ativas de um usuário com paginação |
| `DELETE` | `/transactions/{id}` | Realiza a exclusão lógica (*soft delete*) da transação |

---

## 🧪 Testes Automatizados

A aplicação conta com uma suíte de **testes unitários com Mockito** e testes de inicialização do contexto Spring, garantindo cobertura das regras de negócio sem dependência de banco externo:

```bash
./mvnw test
```

```text
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

<div align="center">

## 👤 Autor

Desenvolvido por **Isaac Gregorio**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/isaacggr)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/isaac-gregorio)

</div>
