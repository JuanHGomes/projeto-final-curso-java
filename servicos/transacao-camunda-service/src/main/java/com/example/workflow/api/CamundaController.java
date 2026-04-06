package com.example.workflow.api;

import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.business.model.Transacao;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("camundaTeste/")
public class CamundaController {
    private final StreamBridge streamBridge;

    public CamundaController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping
    public boolean Start() {
        Transacao transacao = Transacao.builder()
                .numeroConta("123")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Quitanda do tonhao")
                .historico(new LinkedHashMap<>())
                .build();
        return streamBridge.send("transacaoProducer-out-0", transacao);
    }
}
