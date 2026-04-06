package org.example.notificacaoservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificacaoConsumerConfig {
    private final NotificacaoService notificacaoService;

    @Bean
    public Consumer<Message<Notificacao>> notificacaoConsumer(){
        return mensagem -> {
            log.info("Iniciando consumo de mensagem: {}", mensagem.getPayload().toString());
            Notificacao notificacao = mensagem.getPayload();
            notificacaoService.dispararNotificacao(notificacao);
        };
    }
}
