package com.example.workflow.messaging;

import com.example.workflow.business.model.Transacao;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class KafkaConsumer {
    private static final String CORRELATION_MESSAGE = "Nova_Transacao";

    private final RuntimeService runtimeService;

    public KafkaConsumer(RuntimeService runtimeService){
        this.runtimeService = runtimeService;
    }

    public Consumer<Message<Transacao>> transacaoConsumer() {
        return message -> {
            Transacao transacao = message.getPayload();

            Map<String, Object> variaveis = new HashMap<>();

            variaveis.put("TRANSACAO", transacao);

            runtimeService.createMessageCorrelation(CORRELATION_MESSAGE).setVariables(variaveis)
                    .correlateStartMessage();
        };
    }
}
