package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransacaoRestClient {
    private final RestTemplate restTemplate;

    public TransacaoRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void validarFundos(Transacao transacao) {
        restTemplate.get

    }
}
