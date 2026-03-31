package org.example.notificacaoservice.messaging;

import lombok.RequiredArgsConstructor;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class NotificacaoConsumerConfig {
    private final NotificacaoService notificacaoService;

    @Bean
    public Consumer<Notificacao> notificacaoConsumer(){
        return notificacao -> notificacaoService.dispararNotificacao(notificacao);
    }
}
