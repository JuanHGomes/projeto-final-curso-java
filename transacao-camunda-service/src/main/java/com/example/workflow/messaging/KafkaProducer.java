package com.example.workflow.messaging;

import com.example.workflow.business.model.Notificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class KafkaProducer {
    private final StreamBridge streamBridge;

    public boolean sendMessage(String biding, Notificacao notificacao){
        return streamBridge.send(biding, notificacao);
    }
}
