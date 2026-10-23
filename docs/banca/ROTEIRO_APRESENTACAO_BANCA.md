# 🎓 Roteiro Cronometrado para Apresentação da Banca (12 Minutos)

## ⏱️ Minuto a Minuto (Gestão Rigorosa do Tempo)

### 00:00 - 02:00 (2 min) — Contexto do Problema e Premissa Arquitetural
- **Problema**: Dificuldade de alunos universitários encontrarem reforço pontual com professores qualificados, seguros e com preço acessível.
- **Diferencial Inegociável da Engenharia**: Sistema concebido como **Monólito Modular** com **Custo Zero de Nuvem** e **Zero Dependência de APIs Pagas**.
- Destaque: Eliminação de custos recorrentes (LLMs pagos e Google Maps billing) através de **regras determinísticas** e **cálculo de raio por Haversine**.

### 02:00 - 04:30 (2.5 min) — Arquitetura de Software e Pilha Tecnológica
- **Back-end**: Java 17, Spring Boot 3, Spring Security com JWT, Hibernate/JPA, Flyway (V1 a V9 migrations) e PostgreSQL.
- **Mobile**: React Native com Expo, TypeScript, navegação por abas e telas dinâmicas.
- **Segurança & LGPD**: Hash adaptativo BCrypt, separação estrita de papéis (ALUNO, PROFESSOR, ADMIN) e coordenadas aproximadas para privacidade do docente.

### 04:30 - 09:30 (5 min) — Demonstração Prática ao Vivo
1. **Login Rápido**: Aluno Gabriel acessa via credenciais pré-aquecidas (`aluno.banca@orienta.edu.br`).
2. **Busca com Raio de Distância**: Busca presencial por professores num raio de 10 km usando coordenadas locais.
3. **Triagem Determinística (Assistente)**: Demonstração da máquina de estados (Cálculo I ➔ Derivadas ➔ Online até R$ 65/h).
4. **Trava de Segurança F9-T7**: Provar que o assistente **não grava no banco** de forma autônoma — ele gera o Card de Resumo e exige o clique do aluno.
5. **Agendamento e Histórico**: Mostrar aula confirmada e sistema de notas/avaliações já no banco.

### 09:30 - 10:30 (1 min) — Plano de Contingência Offline
- Provar que se o Wi-Fi falhar, o aplicativo e o backend rodam 100% no `localhost:8080` e com o script de reset de 15 segundos (`./scripts/reset-banca-seed.sh`).

### 10:30 - 12:00 (1.5 min) — Conclusão e Transição para Perguntas
- Resultados alcançados: 10 fases concluídas com controle estrito de versão no Git (commits semânticos atômicos).
- Abrir para arguição da banca.

---

## 🎯 Top 5 Perguntas Típicas da Banca e Respostas na Ponta da Língua:
1. **"Por que não usaram uma LLM como OpenAI ou Claude no assistente?"**
   - *Resposta*: Pela diretriz de engenharia de custo zero e previsibilidade. LLMs têm custo por token e risco de alucinar informações de preços ou horários que o professor não possui. A máquina de estados determinística garante 100% de conformidade com os dados do catálogo sem gastar R$ 1,00.

2. **"Como garantiram a conformidade com a LGPD na geolocalização?"**
   - *Resposta*: Nunca armazenamos a latitude e longitude exatas do domicílio do professor no retorno da API pública. Utilizamos apenas coordenadas truncadas de bairro/região e calculamos a distância via fórmula de Haversine no servidor.

3. **"E se o banco corromper ou a conexão falhar durante a banca?"**
   - *Resposta*: Criamos o script `reset-banca-seed.sh` que restaura a massa completa de dados em 15 segundos, além de cache local no mobile para contingência offline.
