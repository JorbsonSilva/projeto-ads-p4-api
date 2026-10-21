# Orienta — API Back-end (Período 4)

API RESTful em **Java 17 e Spring Boot 3.4.5** com **PostgreSQL**, estruturada como um **Monólito Modular** para a plataforma educacional **Orienta**.

---

## 🚀 Como Executar Localmente (Custo Zero)

### 1. Pré-requisitos
* **Java 17 (JDK com `javac`)**
* **Docker e Docker Compose** (para banco de dados e servidor de e-mails local)

---

### 2. Subir os Serviços Locais (PostgreSQL + Mailpit)
Na raiz deste repositório, execute:

```bash
docker compose up -d
```

Isso inicializa automaticamente:
* **PostgreSQL 16**: rodando na porta `5432` (database: `orientadb`, user: `orienta_user`, password: `orienta_secret`)
* **Mailpit (Capturador de E-mails)**:
  * Servidor SMTP: `localhost:1025` (usado pelo Spring Boot para disparar e-mails de validação de cadastro e recuperação de senha)
  * Interface Web: abra [http://localhost:8025](http://localhost:8025) no navegador para ler qualquer e-mail enviado sem precisar de provedor pago.

---

### 3. Executar a Aplicação Spring Boot

No Linux / macOS:
```bash
./mvnw spring-boot:run
```

No Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd spring-boot:run
```

A API estará disponível em: `http://localhost:8080`

---

## 📖 Documentação Interativa da API (Swagger / OpenAPI)

Com o servidor em execução, acesse a documentação viva e teste os endpoints:
* **Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
* **OpenAPI JSON:** [http://localhost:8080/api/v1/api-docs](http://localhost:8080/api/v1/api-docs)

---

## 🗄️ Migrations do Banco de Dados (Flyway)
As alterações de esquema são versionadas em SQL puro na pasta:
`src/main/resources/db/migration/`

* `V1__baseline_schema.sql`: Estrutura base de tabelas do sistema.
O Flyway aplica as migrations automaticamente ao iniciar a aplicação.
