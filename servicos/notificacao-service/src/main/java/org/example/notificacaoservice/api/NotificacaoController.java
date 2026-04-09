package org.example.notificacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.notificacaoservice.business.NotificacaoHub;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController
public class NotificacaoController {
    private final NotificacaoService notificacaoService;
    private final StreamBridge streamBridge;

    @GetMapping("/")
    public String healthCheck(){
        return "Notificacao Service is running";
    }

    @GetMapping(value = "/notificacao", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Notificacao>> dispararNotificacoes(@RequestParam String numeroConta){

        ServerSentEvent<Notificacao> welcome = ServerSentEvent.<Notificacao>builder()
                .data(Notificacao.builder()
                        .numeroConta(numeroConta)
                        .mensagem("Conexão estabelecida com sucesso. Aguardando atividades.")
                        .build())
                .build();

        Flux<ServerSentEvent<Notificacao>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<Notificacao>builder()
                        .comment("heartbeat")
                        .build());

        Flux<ServerSentEvent<Notificacao>> notifications = notificacaoService.receberNotificacao(numeroConta)
                .map(n -> ServerSentEvent.builder(n).build());

        return Flux.just(welcome)
                .concatWith(Flux.merge(notifications, heartbeat))
                .onTerminateDetach();
    }

    @PostMapping("/test")
    public void testarMensagem(@RequestParam(defaultValue = "123") String numeroConta){
        Notificacao notificacaoTeste = Notificacao.builder()
                .numeroConta(numeroConta)
                .mensagem("Notificação de teste enviada às " + java.time.LocalTime.now())
                .build();
        notificacaoService.dispararNotificacao(notificacaoTeste);
    }
}
