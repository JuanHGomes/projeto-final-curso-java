package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransacaoRestClient {
    private static final String ENDPOINT_VALIDAR = "validarFundos";
    private static final String BASE_URL = "http://localhost:8082/transacao/";
    private final RestTemplate restTemplate;

    public TransacaoRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Transacao validarFundos(Transacao transacao) {
        String url = buildUrl(ENDPOINT_VALIDAR);
        Transacao transacaoValidada = restTemplate.postForObject(url, transacao, Transacao.class);

        return transacaoValidada;

    }

    private String buildUrl(String endpoint){
      return BASE_URL+endpoint;
    }

}
