package org.example.notificacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.notificacaoservice.business.NotificacaoHub;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
public class NotificacaoController {
    private final NotificacaoService notificacaoService;

    @GetMapping(value = "/notificacao", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Notificacao> dispararNotificacoes(@RequestParam String numeroConta){
        return notificacaoService.receberNotificacao(numeroConta);
    }
}
