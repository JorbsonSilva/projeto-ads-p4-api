# Log do Projeto - Back-end (Marketplace Educacional)

## Tecnologias e Arquitetura
* **Linguagem:** Java
* **Framework:** Spring Boot
* **Banco de Dados:** PostgreSQL (Hospedado no Supabase)
* **ORM:** Spring Data JPA / Hibernate
* **Ferramentas:** Lombok (Redução de boilerplate)
* **Padrão de Arquitetura:** API RESTful dividida em Camadas (Model, Repository, Service, Controller)

## Status Atual (Sprint 1 - Autenticação Básica)
O núcleo do sistema de usuários e a conexão com o banco de dados via "Code-First" foram estabelecidos. O banco gera as tabelas automaticamente com base no código Java (`ddl-auto=update`).

### Funcionalidades Implementadas
1. **Entidade Usuario (Model):**
   - Tabela `tb_usuarios` criada no Supabase.
   - Colunas: `id` (PK auto-gerado), `nome`, `email` (Unique), `senha`, `tipo` (Aluno/Professor).
   
2. **Acesso a Dados (Repository):**
   - Criação do `UsuarioRepository`.
   - Implementação da Query Method `findByEmail` para buscas personalizadas.

3. **Regras de Negócio (Service):**
   - **Cadastro:** Verifica se o email já existe. Retorna erro ou salva no banco.
   - **Login:** Verifica a existência do email e compara a senha em etapas (Guard Clauses).

4. **Portas de Entrada (Controller):**
   - Rota POST: `/api/usuarios/cadastro` (Recebe JSON e registra usuário).
   - Rota POST: `/api/usuarios/login` (Recebe JSON e valida autenticação).

## Próximos Passos (Back-end)
* [ ] Integrar a chamada da API com o Front-end (React Native).
* [ ] (Futuro) Melhorar o tratamento de exceções (Transformar o erro 500 em um 400 amigável).
* [ ] (Futuro) Implementar criptografia de senha (Bcrypt) e tokens de segurança (JWT).