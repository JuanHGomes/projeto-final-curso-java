package com.example.workflow.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TransacaoProducer {
    private final StreamBridge streamBridge;

    public boolean sendMessage(String biding, Object payload){
        return streamBridge.send(biding, payload);
    }
}
