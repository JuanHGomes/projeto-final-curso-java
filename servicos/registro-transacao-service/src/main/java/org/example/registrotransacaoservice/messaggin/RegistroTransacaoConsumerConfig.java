package org.example.registrotransacaoservice.messaggin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.registrotransacaoservice.business.RegistroTransacaoService;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class RegistroTransacaoConsumerConfig {
    private final RegistroTransacaoService registroTransacaoService;

    @Bean
    public Consumer<Message<Transacao>> registroTransacaoConsumer(){
        return message -> {
           log.info("Mensagem recebida, iniciando processo de garantia de registro da transação");
           registroTransacaoService.garantirRegistroTransacao(message.getPayload());
        };
    }
}
