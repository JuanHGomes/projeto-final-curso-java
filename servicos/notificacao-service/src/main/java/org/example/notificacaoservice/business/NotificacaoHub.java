package org.example.notificacaoservice.business;

import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificacaoHub {
    private final Map<String, Sinks.Many<Notificacao>> sinks = new ConcurrentHashMap<>();

    public Flux<Notificacao> subscribe(String numeroConta){
        System.out.println("Novo subscriber para conta: " + numeroConta);

        sinks.computeIfPresent(numeroConta, (key, oldSink) -> {
            if (oldSink.currentSubscriberCount() == 0) {
                System.out.println("Removendo sink inativo para conta: " + numeroConta);
                return null;
            }
            return oldSink;
        });

        Sinks.Many<Notificacao> sink = sinks.computeIfAbsent(numeroConta,
                it -> Sinks.many().multicast().onBackpressureBuffer(256, false));

        return sink.asFlux()
                .doOnCancel(() -> {
                    System.out.println("Subscriber cancelado para conta: " + numeroConta);
                    cleanupSinkIfNeeded(numeroConta);
                })
                .doOnTerminate(() -> {
                    System.out.println("Subscriber terminado para conta: " + numeroConta);
                    cleanupSinkIfNeeded(numeroConta);
                })
                .doOnError(error -> {
                    System.err.println("Erro no subscriber da conta " + numeroConta + ": " + error.getMessage());
                    cleanupSinkIfNeeded(numeroConta);
                })
                .timeout(Duration.ofMinutes(30))
                .onErrorResume(e -> {
                    System.err.println("Erro recuperado para conta " + numeroConta);
                    return Flux.empty();
                });
    }

    public void publish(Notificacao notificacao){
        System.out.println("Tentando publicar para conta: " + notificacao.numeroConta() + " - Mensagem: " + notificacao.mensagem());
        Sinks.Many<Notificacao> sink = sinks.get(notificacao.numeroConta());

        if(sink != null){
            int subscriberCount = sink.currentSubscriberCount();
            System.out.println("Subscribers ativos: " + subscriberCount);

            if (subscriberCount == 0) {
                System.out.println("Nenhum subscriber ativo, removendo sink para conta: " + notificacao.numeroConta());
                sinks.remove(notificacao.numeroConta());
                return;
            }

            Sinks.EmitResult result = sink.tryEmitNext(notificacao);
            System.out.println("Resultado da emissão: " + result);

            if (result.isFailure()) {
                System.err.println("Falha ao emitir: " + result);
                if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                    cleanupSinkIfNeeded(notificacao.numeroConta());
                }
            }
        } else {
            System.out.println("Nenhum sink encontrado para a conta: " + notificacao.numeroConta());
        }
    }

    private void cleanupSinkIfNeeded(String numeroConta) {
        sinks.computeIfPresent(numeroConta, (key, sink) -> {
            if (sink.currentSubscriberCount() == 0) {
                System.out.println("Limpando sink sem subscribers para conta: " + numeroConta);
                sink.tryEmitComplete();
                return null;
            }
            return sink;
        });
    }
}