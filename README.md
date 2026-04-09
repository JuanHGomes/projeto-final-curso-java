# Projeto Final - Curso Java

## Visão Geral

Sistema bancário digital distribuído, projetado como uma **arquitetura de microsserviços** com comunicação assíncrona via **Apache Kafka**, orquestração de processos com **Camunda BPMN**, armazenamento em memória com **Redis** e persistência em **MongoDB**. O sistema simula transações bancárias (débito e crédito) com validação de fundos, detecção de fraude e geração de extratos/faturas em PDF.

O frontend é uma aplicação **React 18** (via CDN) com **Tailwind CSS**, rodando como arquivo estático no navegador.

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Frontend (React + Tailwind)                  │
│                    index.html — aberto no navegador                  │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ POST /camundaTeste/
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│              transacao-camunda-service :8083                          │
│         Camunda 7.24 + Spring Boot 3.5 + H2 (embedded)              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Workflow BPMN: transacao-process                           │    │
│  │  Validar Fundos → Validar Fraude → Executar Transação      │    │
│  │         → Parallel Gateway → Notificação                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
└──────┬──────────────────────────┬───────────────────────────────────┘
       │ REST                     │ Kafka
       ▼                          ▼
┌──────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│ transacao-service│    │ notificacao-service  │    │registro-transacao-   │
│     :8086        │    │      :8085           │    │    service :8087     │
│ Spring Boot 4    │    │ WebFlux + SSE        │    │ Spring Boot 4        │
│ Redis + MongoDB  │    │ Kafka consumer       │    │ MongoDB + PDF        │
│                  │    │                      │    │ Kafka consumer       │
└──────────────────┘    └──────────┬───────────┘    └──────────┬─────────┘
                                   │                            │
                                   │ Kafka: notificacao-topic   │ Kafka: registro-transacao-topic
                                   ▼                            ▼
                          ┌─────────────────┐          ┌─────────────────┐
                          │  Frontend (SSE)  │          │   MongoDB       │
                          └─────────────────┘          └─────────────────┘
```

### Infraestrutura (Docker Compose)

| Serviço | Imagem | Porta | Finalidade |
|---|---|---|---|
| **Kafka** | confluentinc/cp-kafka:latest | 9092 | Broker de mensagens (KRaft mode) |
| **Kafka UI** | provectuslabs/kafka-ui:latest | 8080 | Interface visual para gerenciar Kafka |
| **Redis** | redis:alpine | 6379 | Saldo, limite e transações pendentes (operações atômicas) |
| **Redis Insight** | redis/redisinsight:latest | 8001 | GUI para Redis |
| **MongoDB** | mongo:7 | 27017 | Persistência de extrato/fatura |

> **Nota:** Os 4 serviços Java rodam **fora do Docker** (direto no localhost).

### Tópicos Kafka

| Tópico | Produtor | Consumidor | Finalidade |
|---|---|---|---|
| `notificacao-topic` | transacao-camunda-service | notificacao-service | Eventos de notificação para entrega SSE ao frontend |
| `registro-transacao-topic` | transacao-camunda-service | registro-transacao-service | Garantia de registro idempotente da transação no MongoDB |

---

## Serviços

### 1. transacao-service (:8086)

Serviço responsável por **validar fundos, detectar fraude e executar transações**. É o serviço de dados central que gerencia saldo e limite via Redis e persiste transações no MongoDB.

**Stack:** Spring Boot 4.0.4 (MVC), Redis, MongoDB, Java 21, Gradle

#### Endpoints

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/transacao/validarFundos` | Valida e aloca fundos. Débito decrementa saldo, crédito decrementa limite. Operação atômica no Redis com reversão automática se ficar negativo. |
| `POST` | `/transacao/validarFraude` | Executa validadores de fraude (transação repetida e intervalo mínimo). |
| `POST` | `/transacao/executarTransacao` | Confirma transação: salva no MongoDB e remove chave pendente do Redis. |
| `POST` | `/transacao/estornarTransacao` | Estorna transação: reverte saldo ou limite no Redis. |
| `GET` | `/transacao/saldo/{numeroConta}` | Consulta saldo atual no Redis. |
| `GET` | `/transacao/limiteCredito/{numeroConta}` | Consulta limite de crédito no Redis. |

#### Modelo de Dados

**Transacao:**
- `numeroConta` (String)
- `valor` (Long — em centavos)
- `tipoTransacao` (DEBITO / CREDITO)
- `timeStamp` (LocalDateTime)
- `estabelecimento` (String)
- `historico` (LinkedHashMap<String, Boolean>) — registro de cada etapa do processo com resultado booleano

**Conta (Redis Hash):**
- Key: `numeroConta:{conta}`
- Fields: `saldo` (Long), `limiteCredito` (Long)

#### Chaves Redis

| Key | Tipo | Descrição | TTL |
|---|---|---|---|
| `numeroConta:{conta}` | Hash | Saldo e limite de crédito | Persistente |
| `transacao:{conta}` | JSON | Transação confirmada | 5 min |
| `confirmacao:{conta}` | JSON | Transação aguardando confirmação | 5 min |

#### Detectores de Fraude (Strategy Pattern)

**TransacaoRepetidaValidator:**
- Detecta fraude se existe transação anterior com **mesmo valor** e **mesmo estabelecimento**.
- Implementa `FraudeValidator.isFraude(Transacao)`.

**TempoEntreTransacoesValidator:**
- Detecta fraude se o intervalo entre a transação atual e a anterior for **≤ 1 minuto**.
- Implementa `FraudeValidator.isFraude(Transacao)`.

#### Camada de Dados

- **`ContaRepositoryRedisImpl`** — Operações atômicas no Redis (`DECRBY`), implementação de `TransacaoOperators.confirmarTransacao()` e `estornarTransacao()` para Redis.
- **`TransacaoRepositoryRedisImpl`** (`@Primary`) — Lê transações do Redis para validação de fraude.
- **`TransacaoRepositoryMongoImpl`** — Salva transações no MongoDB, implementa `TransacaoOperators.confirmarTransacao()` para MongoDB.

---

### 2. transacao-camunda-service (:8083)

Serviço de **orquestração de workflow** usando Camunda BPMN. Recebe a transação do frontend e coordena todo o fluxo: validação de fundos, detecção de fraude, execução, registro e notificação.

**Stack:** Spring Boot 3.5.5, Camunda 7.24.0, Kafka, H2 (embedded), Java 21, Maven

#### Endpoint

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/camundaTeste/` | Recebe `TransacaoRequest`, inicia workflow `transacao-process`. Retorna `boolean` indicando sucesso ou falha da transação. |

#### Workflow BPMN (`transacao-process`)

```
Start Event
    │
    ▼
[Validar Fundos] ──delegate: ValidarFundosDelegate
    │
    ▼
Gateway "Valido?" ── fundosSuficientes == false ──┐
    │                                               │
    │ fundosSuficientes == true                     ▼
    ▼                                          [Gateway de Merge]
[Validar Fraude] ──delegate: ValidarFraudeDelegate     │
    │                                                  │
    ▼                                                  │
Gateway "Valido?" ── isFraude == true ──────────────►  │
    │                                                  │
    │ isFraude == false                                │
    ▼                                                  │
[Executar Transação] ──delegate: ExecutarTransacaoDelegate
    │
    ▼
Gateway "Sucesso?" ── execucaoSucesso == false ──► [Estornar Transação]
    │                                                       │
    │ execucaoSucesso == true                               │
    ▼                                                       │
[Parallel Gateway - Fork]                                   │
    ├──► [Enviar Registro Transação] ── Kafka: registro-transacao-topic
    └──► [Gateway de Merge] ◄──────────────────────────────┘
              │
              ▼
        [Enviar Notificação] ── Kafka: notificacao-topic
              │
              ▼
         End Event
```

#### BPMN Delegates

| Delegate | Função |
|---|---|
| `ValidarFundosDelegate` | Chama REST do transacao-service (`/validarFundos`). Define variável `fundosSuficientes`. |
| `ValidarFraudeDelegate` | Chama REST do transacao-service (`/validarFraude`). Define variável `isFraude`. |
| `ExecutarTransacaoDelegate` | Chama REST do transacao-service (`/executarTransacao`). Define variável `execucaoSucesso`. |
| `EstornarTransacaoDelegate` | Chama REST do transacao-service (`/estornarTransacao`). Reverte saldo/limite. |
| `EnviarRegistroTransacaoDelegate` | Publica transação no Kafka `registro-transacao-topic`. |
| `EnviarNotificacaoDelegate` | Gera mensagem contextual baseada no histórico e publica no Kafka `notificacao-topic`. |

#### Variáveis do Processo Camunda

| Variável | Tipo | Descrição |
|---|---|---|
| `TRANSACAO` | Object | Objeto Transacao completo |
| `fundosSuficientes` | Boolean | Resultado da validação de fundos |
| `isFraude` | Boolean | Resultado da validação de fraude |
| `execucaoSucesso` | Boolean | Resultado da execução da transação |
| `TRANSACAO_REGISTRADA` | Boolean | Confirmação de registro no MongoDB |
| `resultadoTransacao` | Boolean | Resultado final da transação |

#### Camunda Admin

- **URL:** `http://localhost:8083/camunda`
- **Usuário:** `demo` / **Senha:** `demo`

#### Kafka Consumer

- **Binding:** `transacaoConsumer-in-0`
- **Tópico:** `transacao-topic`
- Ao receber mensagem, faz **message correlation** com Camunda usando `Nova_Transacao`, iniciando nova instância do processo.

---

### 3. notificacao-service (:8085)

Serviço que funciona como **ponte entre Kafka e Server-Sent Events (SSE)**. Consome mensagens do Kafka e as distribui para clientes browser conectados via SSE, com um canal multicast independente por número de conta.

**Stack:** Spring Boot 4.0.5 (WebFlux), Kafka, Java 21, Gradle

#### Endpoints

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/` | Health check — retorna `"Notificacao Service is running"`. |
| `GET` | `/notificacao?numeroConta={conta}` | Endpoint SSE. Retorna `Flux<ServerSentEvent<Notificacao>>`. Envia mensagem de boas-vindas, depois merge de notificações reais + heartbeat a cada 15s. Timeout de 30 min por subscriber. |
| `POST` | `/test?numeroConta={conta}` | Envia notificação de teste para a conta especificada (útil para debug). |

#### Arquitetura Interna

**NotificacaoHub** — Pub/Sub reativo baseado em `Sinks.Many` do Project Reactor:

- Mapa `ConcurrentHashMap<String, Sinks.Many<Notificacao>>` — um sink multicast por `numeroConta`.
- **`subscribe(conta)`**: Cria ou reusa sink. Remove sinks inativos (0 subscribers). Timeout de 30 minutos.
- **`publish(notificacao)`**: Emite para o sink da conta. Se 0 subscribers, remove o sink automaticamente.
- Buffer de backpressure: 256 elementos.

#### Kafka Consumer

- **Binding:** `notificacaoConsumer-in-0`
- **Tópico:** `notificacao-topic`
- Ao receber, chama `notificacaoService.dispararNotificacao()` que publica no sink SSE da conta correspondente.

#### Notificações Geradas

O serviço de notificação gera mensagens contextuais baseadas na etapa do workflow e resultado:

| Etapa | Resultado | Mensagem |
|---|---|---|
| `FUNDOS_SUFICIENTES` | `true` | `Pagamento em {estabelecimento} realizado com sucesso!` |
| `FUNDOS_SUFICIENTES` | `false` | `Pagamento em {estabelecimento} recusado: fundos insuficientes.` |
| `FRAUDE` | `true` | `Pagamento em {estabelecimento} aprovado após verificação.` |
| `FRAUDE` | `false` | `Pagamento em {estabelecimento} bloqueado: possível fraude detectada.` |
| `EXECUCAO_SUCESSO` | — | `Transação em {estabelecimento} concluída com sucesso!` |

---

### 4. registro-transacao-service (:8087)

Serviço responsável pelo **registro persistente de transações no MongoDB**, geração de **extrato bancário** e **fatura de cartão de crédito em PDF**, e consulta de saldo/limite (proxy para transacao-service).

**Stack:** Spring Boot 4.0.5 (MVC), MongoDB, Kafka, Thymeleaf, OpenHTMLtoPDF, Java 21, Gradle

#### Endpoints

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/registro-transacao/extrato/{numeroConta}` | Retorna extrato dos últimos 30 dias (transações do MongoDB). |
| `GET` | `/registro-transacao/saldo/{numeroConta}` | Proxy — consulta saldo no transacao-service via REST. |
| `GET` | `/registro-transacao/limiteCredito/{numeroConta}` | Proxy — consulta limite no transacao-service via REST. |
| `GET` | `/registro-transacao/extratoPdf/{numeroConta}` | Gera e retorna PDF do extrato bancário (download direto). |
| `GET` | `/registro-transacao/faturaPdf/{numeroConta}` | Gera e retorna PDF da fatura de cartão de crédito (apenas transações CREDITO). |
| `POST` | `/registro-transacao/enviarExtratoEmail/{numeroConta}` | Mock de envio por email (apenas log). |

#### Geração de PDF

- Usa **Thymeleaf** para renderizar templates HTML + **OpenHTMLtoPDF** para converter em PDF.
- **`extrato-bancario.html`** — Extrato de conta corrente com tabela de transações, cores por tipo (vermelho = débito, verde = crédito).
- **`extrato-cartao.html`** — Fatura de cartão de crédito com resumo, cartão final, vencimento e limite.

#### Kafka Consumer

- **Binding:** `registroTransacaoConsumer-in-0`
- **Tópico:** `registro-transacao-topic`
- Ao receber, chama `registroTransacaoService.garantirRegistroTransacao()` — operação **idempotente** (verifica por conta + timestamp antes de salvar).

#### Camada de Dados

- **`RegistroTransacaoDao`** — Interface `MongoRepository<TransacaoDocument, String>` com métodos derivados:
  - `findTransacaoDocumentByNumeroContaAndTimeStamp`
  - `findByNumeroContaAndTimeStampBetween`
- **`RegistroTransacaoRepository`**:
  - `registrarSeNaoPresente()` — Verifica se transação já existe, se não, salva.
  - `findAllOverLastThirtyDaysByNumeroConta()` — Consulta por conta e range de 30 dias.

---

## Fluxo Completo de uma Transação

1. **Frontend** envia `POST` para `http://localhost:8083/camundaTeste/` com dados da transação (número da conta, valor, tipo, estabelecimento, timestamp).

2. **Camunda Controller** recebe a requisição e inicia uma nova instância do workflow `transacao-process`.

3. **Validar Fundos** — Delegate chama REST do transacao-service (`/validarFundos`):
   - Débito: `DECRBY` no saldo. Crédito: `DECRBY` no limite.
   - Se ficar negativo, reverte e retorna `fundosSuficientes = false`.
   - Se `false`, o workflow pula direto para notificação de recusa.

4. **Validar Fraude** — Delegate chama REST do transacao-service (`/validarFraude`):
   - Executa `TransacaoRepetidaValidator` (mesmo valor + mesmo estabelecimento).
   - Executa `TempoEntreTransacoesValidator` (intervalo ≤ 1 minuto).
   - Se fraude, executa **Estornar Transação** (reverte Redis) → notificação de bloqueio.

5. **Executar Transação** — Delegate chama REST do transacao-service (`/executarTransacao`):
   - Salva transação no MongoDB.
   - Remove chave pendente do Redis.

6. **Parallel Gateway (Fork):**
   - **Branch 1:** `EnviarRegistroTransacaoDelegate` → publica no Kafka `registro-transacao-topic`.
   - **Branch 2:** Converge diretamente.

7. **Enviar Notificação** — `EnviarNotificacaoDelegate` gera mensagem contextual e publica no Kafka `notificacao-topic`.

8. **notificacao-service** consome do Kafka → entrega via SSE ao frontend conectado em tempo real.

9. **registro-transacao-service** consome do Kafka → salva no MongoDB (idempotente).

---

## Frontend

**Arquivo:** `frontend/index.html`

- **React 18** via CDN (Babel standalone para transpilação JSX no browser).
- **Tailwind CSS** via CDN.
- **SSE** — Conexão manual via botão "Conectar"/"Desconectar".
- **Notificações visuais** — Toasts coloridos dependendo do tipo:
  - 🟢 `sucesso` — Notificações de simulação aprovada.
  - 🔴 `erro` — Notificações de simulação reprovada ou erros.
  - 🟡 `warn` — Avisos de campos obrigatórios.
  - 🔵 `default` — Notificações SSE do backend.
- **Posição das notificações:**
  - **Esquerda** — Notificações de simulação de transação.
  - **Direita** — Notificações SSE vindas do backend.
- **Logging de console** — Prefixos `[Extrato]`, `[Saldo]`, `[Limite]` para facilitar debug.

### Formulário de Simulação

- Campos: Número da Conta Destino, Estabelecimento, Data e Hora, Valor (R$), Tipo (DÉBITO / CRÉDITO).
- Botão "Simular Compra" com spinner de processamento durante a requisição.

### Seções da Interface

- **Minha Conta** — Campo para número da conta + botão Conectar/Desconectar SSE.
- **Saldo em Conta** — Exibição do saldo atual.
- **Limite de Crédito** — Exibição do limite disponível.
- **Simular Transação** — Formulário completo para simulação.
- **Extrato Bancário** — Tabela de transações dos últimos 30 dias com opções de download PDF e envio por email.
- **Fatura Cartão de Crédito** — Seção escura com total de compras no cartão em aberto.

---

## Regras de Negócio

### Transações

1. **Tipos de transação:** DÉBITO (debita saldo) e CRÉDITO (debita limite).
2. **Valores são armazenados em centavos** (Long) para evitar problemas de precisão com ponto flutuante.
3. **Fundos insuficientes:** Se o saldo/limite ficar negativo após a transação, a operação é revertida automaticamente no Redis.
4. **Estorno:** Se a transação falhar após validação de fundos, o valor é estornado (revertido) no Redis.

### Detecção de Fraude

5. **Transação repetida:** Uma transação com **mesmo valor** e **mesmo estabelecimento** que uma transação anterior é classificada como fraude.
6. **Intervalo mínimo:** Transações com intervalo **≤ 1 minuto** entre si são classificadas como fraude.
7. Se fraude detectada, a transação é **estornada** (saldo/limite revertido) e uma notificação de bloqueio é enviada.

### Registro

8. **Idempotência:** O registro de transação no MongoDB é idempotente — verifica por `numeroConta` + `timeStamp` antes de salvar, evitando duplicatas.
9. **Persistência:** Transações confirmadas são salvas no MongoDB com collection `transacoes`.

### Notificações

10. **Entrega em tempo real:** Notificações são entregues via SSE (Server-Sent Events) ao frontend conectado.
11. **Timeout de conexão:** Conexões SSE expiram após 30 minutos sem atividade.
12. **Heartbeat:** O servidor envia um heartbeat a cada 15 segundos para manter a conexão ativa.
13. **Canais isolados:** Cada `numeroConta` tem seu próprio canal multicast independente.

### Extrato e Fatura

14. **Extrato:** Inclui todas as transações (DÉBITO e CRÉDITO) dos últimos 30 dias.
15. **Fatura:** Inclui apenas transações do tipo CRÉDITO.
16. **PDFs:** Gerados a partir de templates HTML (Thymeleaf) convertidos via OpenHTMLtoPDF.

---

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| **Backend** | Spring Boot 3.5.5 / 4.0.5, Java 21 |
| **BPMN / Workflow** | Camunda 7.24.0 |
| **Message Broker** | Apache Kafka (KRaft mode) |
| **Cache / State** | Redis (operações atômicas) |
| **Database** | MongoDB 7 |
| **Frontend** | React 18, Tailwind CSS, Babel (CDN) |
| **Build** | Gradle (3 serviços), Maven (1 serviço) |
| **PDF** | Thymeleaf + OpenHTMLtoPDF |
| **Comunicação** | REST, Kafka, Server-Sent Events (SSE) |
