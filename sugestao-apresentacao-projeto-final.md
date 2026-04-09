# Sugestão de Apresentação — Projeto Final

> Este guia foi pensado para maximizar sua nota nos critérios de **Demo**, **Arquitetura**, **Perguntas técnicas** e **Organização/tempo** — que juntos valem 1,5 pontos — e para reforçar a percepção de qualidade nos critérios de código (6,5 pontos).

---

## Estrutura sugerida

O tempo base é **10 minutos**. Se precisar de mais tempo, é possível estender até **15 minutos** conforme a coluna de extensão abaixo.

| Bloco | 10 min (base) | 15 min (extensão) | O que cobrir |
|---|---|---|---|
| 1. Abertura + Arquitetura | 2 min | 3 min | Problema de negócio + diagrama de serviços |
| 2. Demo ao vivo | 6 min | 8 min | Fluxo completo ponta a ponta |
| 3. Decisões técnicas | — | 2 min | Um ponto relevante por componente |
| 4. Testes + Encerramento | 2 min | 2 min | JaCoCo, reflexão final |

> **Regra de ouro para 10 minutos:** se algum bloco atrasar, **corte da demo**, não do encerramento. Terminar com reflexão demonstra maturidade; terminar no meio de um log de console não.

---

## Bloco 1 — Abertura + Arquitetura (2 min / 3 min na extensão)

Não comece pelo código. Comece pelo **porquê** e mostre o mapa antes de entrar no território.

**Abertura (~40 seg):**
- "Nosso projeto simula uma plataforma financeira que processa transações com segurança, notifica o cliente em tempo real e gera extrato em PDF."
- Mencione a stack: Spring Boot, Kafka, Redis, MongoDB, Camunda, SSE.

**Diagrama de arquitetura (~1 min 20 seg):**

Apresente **um único diagrama** com todos os serviços e suas conexões:

```
[Cliente HTTP] → [Serviço de Tx] → [Redis]
                       ↓
                    [Kafka]
                       ↓
                   [Camunda]
                  ↙         ↘
[Serviço de Notificações]  [Serviço de Faturas/Extrato]
         ↓                           ↓
        SSE                      [MongoDB]
         ↓
[Frontend React]
```

- Use cores para separar camadas: transporte (Kafka/SSE), persistência (Redis/Mongo), orquestração (Camunda).
- Mostre a direção dos dados com setas.
- Não explique cada serviço em detalhe — a demo faz isso. Aqui é só o mapa.

---

## Bloco 2 — Demo ao vivo (6 min / 8 min na extensão) ⭐

Este é o bloco mais importante. Siga um **roteiro fixo** e ensaie pelo menos duas vezes antes.

Para caber em 6 minutos, cada passo tem **~1 minuto**. Não tente mostrar tudo — mostre **uma coisa que funciona por componente**.

### Roteiro de demo (6 min)

**1. Transação + Atomicidade — 1 min**
- POST via Postman ou curl.
- Mostre no log que a operação de débito é **atômica** — o ponto não é o Redis em si, mas a garantia de atomicidade. Redis é a abordagem prioritária (via `DECRBY`); o MongoDB também suporta isso via `findAndModify`, mas Redis é a escolha principal.
- Se tiver cenário de saldo insuficiente: demonstre o bloqueio em 10 segundos.

**2. Kafka — 1 min**
- Mostre no log do consumidor que o evento chegou no tópico correto.
- Fale o nome do tópico em voz alta — isso conta na avaliação.

**3. Camunda — 1,5 min**
- Abra o Camunda Cockpit com o processo já em andamento (não espere subir ao vivo).
- Mostre pelo menos uma transição de estado no diagrama BPMN.
- Se tiver tratamento de falha, é o momento de mencionar.

**4. MongoDB + PDF — 1 min**
- Mostre o documento de fatura no Compass ou via API.
- Faça o download do PDF — abrir o arquivo ao vivo causa impacto.

**5. SSE — 1,5 min**
- Tenha o frontend React já aberto no browser antes de começar.
- Execute uma transação e mostre a notificação chegando sem refresh.
- Este é o momento mais visual — deixe o público ver a tela.

### Se a extensão for liberada (8 min)

Use os 2 minutos extras para aprofundar o Camunda (mostrar erro e compensação) ou o Redis (mostrar a chave sendo criada e expirada no Redis CLI).

### Checklist de preparação para o dia

- [ ] Todos os serviços rodando (Docker Compose testado do zero na véspera)
- [ ] Postman Collection com as requisições nomeadas e em ordem
- [ ] Camunda Cockpit aberto e logado com um processo já iniciado
- [ ] MongoDB Compass ou endpoint de consulta pronto
- [ ] Frontend rodando, SSE já conectado
- [ ] Terminal com logs visíveis (fonte ≥ 14pt, fundo escuro)
- [ ] PDF já gerado e salvo — não dependa de gerar ao vivo se for lento
- [ ] **Plano B:** vídeo gravado da demo completa caso algo falhe

---

## Bloco 3 — Decisões técnicas (apenas na extensão — 2 min)

Se o tempo for extendido para 15 min, use este bloco para explicar **uma decisão por componente** em uma frase focada no *porquê*, não no *como*:

| Componente | Exemplo de decisão a destacar |
|---|---|
| Redis | "A prioridade é garantir **atomicidade** no débito — Redis é a escolha principal via `DECRBY`, que é atômico por natureza. MongoDB também suporta atomicidade via `findAndModify`, mas Redis é nossa abordagem prioritária" |
| Kafka | "Chave de partição = `contaId` para garantir ordem de eventos por conta" |
| Camunda | "Usamos User Task para aprovação manual de transações suspeitas e incidentes para tratar falhas no fluxo sem derrubar o processo" |
| MongoDB | "Aggregation pipeline para consolidar o extrato antes de renderizar o PDF" |
| SSE | "`SseEmitter` com timeout configurado e reconexão automática no frontend" |

> Na versão de 10 min, essas decisões devem ser ditas **dentro da demo**, em uma frase rápida ao mostrar cada componente.

---

## Bloco 4 — Testes + Encerramento (2 min)

**Testes (~1 min):**
- Mostre o relatório de cobertura (JaCoCo ou relatório de cobertura da IDE) — precisa estar ≥ 65%.
- Mencione o tipo de teste priorizado: unitário, integração.
- Se estiver abaixo de 65%, diga o que cobriu — não esconda.

**Encerramento (~1 min):**
- O que foi mais difícil e como resolveu.
- O que faria diferente com mais tempo.

Essa reflexão alimenta diretamente a **nota do professor** (avaliação subjetiva, peso ×0,10).

---

## Preparação para perguntas técnicas

As perguntas serão feitas **ao final de todas as apresentações** — mas esteja pronto para responder perguntas no meio da sua também, caso o professor ou os convidados interrompam. Prepare respostas para os componentes de maior peso:

> O professor e os convidados podem perguntar sobre qualquer componente. As perguntas abaixo são as mais prováveis — conheça as respostas, não as decore.

**Redis (×0,20 — maior peso)**

> O ponto central não é o Redis em si — é a **garantia de atomicidade**. Redis é a solução prioritária, mas o MongoDB também viabiliza operações atômicas. Saiba defender a escolha técnica, não apenas o nome da tecnologia.

- Como você garantiu atomicidade na operação de débito?
- Por que Redis é a escolha prioritária para isso? O MongoDB não resolveria?
- O que acontece se o Redis cair durante a transação?
- Por que não banco relacional com `SELECT FOR UPDATE`?

**Camunda (×0,15)**
- Qual é o modelo BPMN do seu processo? Mostre no Cockpit.
- Como você tratou falhas? Tem User Task ou incidente no diagrama?
- Como o Camunda se comunica com os outros serviços?

**Kafka (×0,10)**
- Qual é o nome do tópico? Quantas partições?
- O consumidor é idempotente? O que acontece se a mesma mensagem chegar duas vezes?
- Você usou chave de partição? Por quê?

**MongoDB (×0,10)**
- Como está modelado o documento de fatura?
- Usou índices? Quais campos?
- Como gerou o PDF? Qual biblioteca?

**SSE (×0,05)**
- O que é SSE e por que não usou WebSocket?
- Como o frontend reconecta se a conexão cair?

---

## Dicas gerais

- **Ensaie o cronômetro**: faça uma apresentação completa com timer. Saber onde você está no tempo evita cortes de última hora.
- **Logs visíveis**: mostrar o sistema funcionando nos logs vale tanto quanto a demo visual.
- **Não peça desculpas** por funcionalidades faltando — explique a priorização.
- **Fale sobre o que não está pronto** antes de o professor ou os convidados perguntarem — demonstra maturidade técnica.
- A apresentação é **individual** — você apresenta tudo sozinho. Ensaie os componentes que domina menos para não travar.
