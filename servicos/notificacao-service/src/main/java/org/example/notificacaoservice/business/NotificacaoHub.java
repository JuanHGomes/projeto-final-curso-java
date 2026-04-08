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
        System.out.println("Novo subscriber para conta: " + numeroConta);
        Sinks.Many<Notificacao> sink = sinks.computeIfAbsent(numeroConta,
                it -> Sinks.many().multicast().onBackpressureBuffer());

        return sink.asFlux();
    }

    public void publish(Notificacao notificacao){
        System.out.println("Tentando publicar para conta: " + notificacao.numeroConta() + " - Mensagem: " + notificacao.mensagem());
        Sinks.Many<Notificacao> sink = sinks.get(notificacao.numeroConta());

        if(sink != null){
            Sinks.EmitResult result = sink.tryEmitNext(notificacao);
            System.out.println("Resultado da emissão: " + result);
        } else {
            System.out.println("Nenhum subscriber encontrado para a conta: " + notificacao.numeroConta());
        }
    }
}
