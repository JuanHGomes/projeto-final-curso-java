package org.example.notificacaoservice.business;

import lombok.RequiredArgsConstructor;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class NotificacaoService {
    private final NotificacaoHub hub;

    public Flux<Notificacao> receberNotificacao(String numeroConta) {

        return hub.subscribe(numeroConta);
    }

    public void dispararNotificacao(Notificacao notificacao) {
        hub.publish(notificacao);
    }
}
