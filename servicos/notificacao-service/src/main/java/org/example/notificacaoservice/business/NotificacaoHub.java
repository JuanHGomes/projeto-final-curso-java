package org.example.notificacaoservice.business;

import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificacaoHub {
    private final Map<String, Sinks.Many<Notificacao>> sinks = new ConcurrentHashMap<>();

    public Flux<Notificacao> subscribe(String numeroConta){
        Sinks.Many<Notificacao> sink = sinks.computeIfAbsent(numeroConta,
                it -> Sinks.many().multicast().onBackpressureBuffer(100, false));

        return sink.asFlux();
    }

    public void publish(Notificacao notificacao){
        Sinks.Many<Notificacao> sink = sinks.get(notificacao.numeroConta());

        if(sink != null){
            sink.tryEmitNext(notificacao);
        }
    }
}
