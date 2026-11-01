# Diretrizes de Desenvolvimento - Finance App

Este arquivo fornece diretrizes e contexto básico para agentes de IA e desenvolvedores que colaboram no projeto.

## 1. Visão Geral & Stack
- **Projeto:** API REST de controle e gestão financeira pessoal.
- **Stack Principal:**
  - Java 21+ / Spring Boot 3+
  - Spring Data JPA / Hibernate
  - MySQL 8.0 (gerenciado via Docker Compose)
  - Maven (`./mvnw`)
  - JUnit 5 & Mockito para testes

## 2. Padrões de Arquitetura
A aplicação segue uma arquitetura em camadas estritas:
- `controller`: Endpoints REST, mapeamento de rotas e injeção de dependências.
- `service`: Concentra 100% das regras de negócio, validações de domínio e controle de transações (`@Transactional`).
- `repository`: Interfaces Spring Data JPA para comunicação e consultas ao banco.
- `domain.entity`: Entidades mapeadas para o banco com métodos de domínio e soft delete (`deleted = true`).
- `dto`: `records` imutáveis para transporte de dados (Request e Response), validados com Bean Validation (`jakarta.validation`).

## 3. Boas Práticas & Segurança
- **Zero Segredos no Código:** Credenciais, senhas e chaves devem ser lidas exclusivamente de variáveis de ambiente (`.env`).
- **Validação:** Todas as requisições de entrada devem ser validadas nos DTOs (`@Valid`).
- **Tratamento de Exceções:** Erros de negócio devem lançar exceções tratadas pelo `GlobalExceptionHandler`.
- **Testes:** Manter a suíte de testes (`./mvnw test`) sempre verde após qualquer alteração.
- **Comunicação:** Manter comentários de código e respostas ao usuário em Português (pt-BR).

