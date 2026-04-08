package com.example.workflow.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransacaoProducer {
    private final StreamBridge streamBridge;

    public boolean sendMessage(String biding, Object payload){
        log.info("Enviando mensagem.");
        return streamBridge.send(biding, payload);
    }
}
