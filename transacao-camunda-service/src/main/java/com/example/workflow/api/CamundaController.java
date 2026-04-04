package com.example.workflow.api;

import com.example.workflow.business.model.Transacao;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("camunda/")
public class CamundaController {
    private final StreamBridge streamBridge;

    public CamundaController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void Start(){
        Transacao tranascao = Transacao.builder()
        streamBridge.send("transacaoProducer-out-0", transacao);
}
