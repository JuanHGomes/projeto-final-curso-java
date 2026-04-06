package com.example.workflow.messaging;

import com.example.workflow.business.model.Transacao;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class TransacaoConsumerConfig {
    private static final String CORRELATION_MESSAGE = "Nova_Transacao";

    private final RuntimeService runtimeService;

    public TransacaoConsumerConfig(RuntimeService runtimeService){
        this.runtimeService = runtimeService;
    }

    @Bean
    public Consumer<Message<Transacao>> transacaoConsumer() {
        return message -> {
            log.info("Mensagem recebida, iniciando o Camunda");
            Transacao transacao = message.getPayload();

            Map<String, Object> variaveis = new HashMap<>();

            variaveis.put("TRANSACAO", transacao);

            runtimeService.createMessageCorrelation(CORRELATION_MESSAGE).setVariables(variaveis)
                    .correlateStartMessage();
        };
    }
}
