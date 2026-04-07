package org.example.notificacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.notificacaoservice.business.NotificacaoHub;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController
public class NotificacaoController {
    private final NotificacaoService notificacaoService;
    private final StreamBridge streamBridge;

    @GetMapping(value = "/notificacao", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Notificacao> dispararNotificacoes(@RequestParam String numeroConta){
        return notificacaoService.receberNotificacao(numeroConta);
    }

    @PostMapping
    public void testarMensagem(){
        Notificacao notificacaoTeste = Notificacao.builder()
                .numeroConta("123")
                .mensagem("teste")
                .build();
        streamBridge.send("notificacaoProducer-out-0", notificacaoTeste);
    }
}
