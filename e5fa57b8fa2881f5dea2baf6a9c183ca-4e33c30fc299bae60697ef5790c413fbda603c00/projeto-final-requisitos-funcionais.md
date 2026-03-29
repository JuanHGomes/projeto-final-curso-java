# Requisitos funcionais — Projeto Final


---

## 1. Usuário (ator)

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-U01 | O usuário deve poder iniciar uma transação de **crédito** (modalidade de pagar depois). | Essencial |
| RF-U02 | O usuário deve poder iniciar uma transação de **débito** (saída de valor). | Essencial |
| RF-U03 | O usuário deve receber notificação em tempo (quase) real quando uma transação for processada (aprovada e registrada). | Essencial |

---

## 2. Serviço de Transações (API Spring)

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-ST01 | O serviço deve receber a solicitação de transação (crédito ou débito) com identificação do usuário/conta e valor. | Essencial |
| RF-ST02 | O serviço deve consultar e atualizar no Redis, por operação atômica (ex.: DECR/DECRBY): o **saldo da conta** para transações de **débito** e o **limite** (cartão de crédito) para transações de **crédito**. | Essencial |
| RF-ST03 | Em caso de **saldo suficiente** (débito) ou **limite suficiente** (crédito), o serviço deve considerar a transação **aprovada**. | Essencial |
| RF-ST04 | Em caso de **saldo insuficiente** (débito) ou **limite insuficiente** (crédito), o serviço deve considerar a transação **reprovada**. | Essencial |
| RF-ST05 | O serviço deve publicar mensagem da transação **aprovada** no tópico Kafka definido para o fluxo principal. | Essencial |
| RF-ST06 | O serviço deve publicar mensagem da transação **reprovada** no tópico Kafka definido para o fluxo de reprovadas (TBD). | Essencial |
| RF-ST07 | A mensagem publicada no Kafka deve conter dados mínimos para processamento posterior (ex.: id da transação, id usuário/conta, valor, tipo crédito/débito, timestamp, status aprovado/reprovado). | Essencial |
| RF-ST08 | O serviço deve responder ao solicitante (usuário/cliente) com o resultado da autorização (aprovado ou reprovado) de forma síncrona. | Essencial |

---

## 3. Redis

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-R01 | O Redis deve armazenar o **saldo da conta** (para autorização de transações de débito) e o **limite** do cartão de crédito (para autorização de transações de crédito), por chave (ex.: conta). | Essencial |
| RF-R02 | O Redis deve suportar operações **atômicas** (ex.: DECR ou DECRBY) sobre saldo e sobre limite para garantir consistência em concorrência. | Essencial |
| RF-R03 | Saldo e limite devem ser armazenados como valor numérico compatível com o domínio financeiro (ex.: inteiros em centavos ou decimal). | Essencial |

---

## 4. Kafka

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-K01 | O Kafka deve expor um **tópico** para transações **aprovadas**, consumido pelo Camunda (fluxo principal). | Essencial |
| RF-K02 | O Kafka deve expor um **tópico** para transações **reprovadas**, para fluxo futuro (TBD). | Essencial |
| RF-K03 | As mensagens devem ser **persistidas** com durabilidade configurada (ex.: replicação e acknowledgment) para não perder transações autorizadas. | Essencial |
| RF-K04 | O Kafka deve permitir **consumo** das mensagens em ordem (por partição) para o fluxo de transações aprovadas. | Essencial |
| RF-K05 | O Kafka deve expor um tópico para **eventos de notificação** produzidos pelo Camunda e consumidos pelo Serviço de Notificações. | Essencial |

**Obs. RF-K05:** Depende da arquitetura definida. Se o Camunda invocar **diretamente** o Serviço de Notificações (sem passar pelo Kafka), não há necessidade desse tópico; nesse caso o RF-K05 não se aplica.

---

## 5. Camunda (workflow)

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-C01 | O Camunda deve **consumir** mensagens do tópico Kafka de transações aprovadas e iniciar uma instância de processo (workflow) por transação. | Essencial |
| RF-C02 | O workflow deve **invocar o Serviço de Faturas/Extrato** para registrar a transação no extrato/fatura (persistência no MongoDB). | Essencial |
| RF-C03 | O workflow deve **aguardar** confirmação de sucesso da persistência no Serviço de Faturas/Extrato antes de prosseguir. | Essencial |
| RF-C04 | Após o registro no extrato/fatura, o workflow deve **publicar evento de notificação** no tópico Kafka de notificações (ou invocar diretamente o Serviço de Notificações, conforme definição de arquitetura). | Essencial |
| RF-C05 | O payload do evento de notificação deve conter dados necessários para o usuário (ex.: id transação, valor, tipo, data/hora, resumo). | Essencial |
| RF-C06 | O workflow deve tratar falhas (ex.: retry ou dead-letter) na chamada ao Serviço de Faturas/Extrato ou na publicação de notificação. | Desejável |
| RF-C07 | O Camunda não deve consumir (ou deve tratar à parte) o tópico de transações reprovadas até que o fluxo TBD esteja definido. | Essencial |

---

## 6. Serviço de Faturas/Extrato

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-F01 | O serviço deve expor operação (API ou consumidor Kafka) para **registrar** uma transação no extrato/fatura do usuário/conta. | Essencial |
| RF-F02 | O serviço deve **persistir** no MongoDB os dados da transação (ex.: id, conta/usuário, valor, tipo crédito/débito, data/hora, status). | Essencial |
| RF-F03 | O registro deve ser associado à **conta/usuário** correto para compor extrato e fatura. | Essencial |
| RF-F04 | O serviço deve responder sucesso ou falha ao chamador (Camunda) para permitir controle do workflow. | Essencial |
| RF-F05 | O serviço deve permitir **consulta** de extrato/fatura por conta/usuário e período (para uso por frontend ou outros sistemas). | Essencial |
| RF-F06 | O serviço deve **gerar PDF** de extrato ou fatura (por conta/usuário e período), para download ou envio ao usuário. | Essencial |

---

## 7. Serviço de Notificações

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-N01 | O serviço deve **consumir** eventos de notificação (do tópico Kafka ou chamada do Camunda) para envio ao usuário. | Essencial |
| RF-N02 | O serviço deve identificar o **usuário/conta** destinatário a partir do evento e enviar a mensagem ao canal apropriado (SSE via Frontend). | Essencial |
| RF-N03 | O conteúdo da notificação deve incluir informações da transação (ex.: valor, tipo, data/hora) em formato adequado ao usuário. | Essencial |
| RF-N04 | O serviço deve integrar-se ao **Frontend de Notificações** (SSE) para entrega em tempo (quase) real. | Essencial |
| RF-N05 | Em caso de usuário offline, o sistema pode armazenar notificação para entrega posterior ou descartar, conforme regra de negócio (TBD). | Desejável |

---

## 8. Frontend de Notificações

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-FN01 | O frontend deve manter conexão **SSE** (Server-Sent Events) com o Serviço de Notificações (ou gateway) para receber eventos em tempo real. | Essencial |
| RF-FN02 | O frontend deve **exibir** ao usuário as notificações recebidas (ex.: “Transação de débito de R$ X processada”). | Essencial |
| RF-FN03 | O frontend deve identificar o **usuário** na conexão SSE para receber apenas notificações da sua conta. | Essencial |
| RF-FN04 | O frontend deve reconectar automaticamente em caso de queda da conexão SSE. | Desejável |
| RF-FN05 | O frontend pode manter histórico local (session/local) das notificações da sessão para exibição. | Desejável |

---

## 9. MongoDB

| ID | Requisito | Prioridade |
|----|-----------|------------|
| RF-M01 | O MongoDB deve **persistir** os registros de transações (extrato/fatura) escritos pelo Serviço de Faturas/Extrato. | Essencial |
| RF-M02 | Os documentos devem permitir consulta por **conta/usuário** e por **período** para geração de extrato e fatura. | Essencial |
| RF-M03 | Os dados armazenados devem incluir: identificador da transação, conta/usuário, valor, tipo (crédito/débito), data/hora, e demais campos definidos pelo Serviço de Faturas/Extrato. | Essencial |
| RF-M04 | A persistência deve ser durável (write concern adequado) para não perder transações já autorizadas. | Essencial |

---

## Resumo por componente

| Componente | Quantidade de RF (Essenciais) | Observação |
|------------|-------------------------------|------------|
| Usuário | 3 | Ator; requisitos de capacidade do sistema em nome do usuário. |
| Serviço de Transações | 8 | Núcleo da autorização e publicação no Kafka. |
| Redis | 3 | Saldo da conta (débito), limite (crédito) e operações atômicas. |
| Kafka | 5 | Tópicos e durabilidade. |
| Camunda | 5 (+ 2 desejável) | Workflow extrato + notificação. |
| Serviço de Faturas/Extrato | 6 | Persistência em MongoDB, consulta e geração de PDF. |
| Serviço de Notificações | 4 (+ 1 desejável) | Consumo de eventos e entrega via SSE. |
| Frontend de Notificações | 3 (+ 2 desejáveis) | Conexão SSE e exibição. |
| MongoDB | 4 | Persistência de extrato/fatura. |

---

## TBD / Pendências

- Detalhar **fluxo de transações reprovadas** (consumidor Kafka, regras, notificação ao usuário) e refletir em novos RF se necessário.
- Definir se notificação ao usuário em caso de **reprovação** é requisito (e em qual componente).
- Regra de notificações para usuário **offline** (armazenar, descartar, reenviar).

---
