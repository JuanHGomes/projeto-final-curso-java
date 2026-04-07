package com.example.workflow.api;

import com.example.workflow.api.model.TransacaoRequest;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("camundaTeste/")
public class CamundaController {
    private final StreamBridge streamBridge;
    private final TransacaoMapper mapper;

    @PostMapping
    public boolean Start(@RequestBody TransacaoRequest request) {
        Transacao transacao =  mapper.toTransacao(request);
        return streamBridge.send("transacaoProducer-out-0", transacao);
    }
}
