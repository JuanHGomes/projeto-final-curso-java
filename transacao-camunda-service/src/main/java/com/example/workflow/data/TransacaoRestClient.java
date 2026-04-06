package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransacaoRestClient {
    private static final String ENDPOINT_VALIDAR_FUNDOS = "validarFundos";
    private static final String ENDPOINT_VALIDAR_FRAUDE = "validarFraude";
    private static final String ENDPOINT_EXECUTAR_TRANSACAO = "executarTransacao";

    private static final String BASE_URL = "http://localhost:8082/transacao/";
    private final RestTemplate restTemplate;

    public TransacaoRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Transacao validarFundos(Transacao transacao) {
        String url = buildUrl(ENDPOINT_VALIDAR_FUNDOS);
        Transacao transacaoValidada = restTemplate.postForObject(url, transacao, Transacao.class);

        return transacaoValidada;
    }

    private String buildUrl(String endpoint){
      return BASE_URL+endpoint;
    }

    public Transacao validarFraude(Transacao transacao) {
        String url = buildUrl(ENDPOINT_VALIDAR_FRAUDE);
        Transacao transacaoValidada = restTemplate.postForObject(url, transacao, Transacao.class);

        return transacaoValidada;
    }

    public Transacao executarTransacao(Transacao transacao) {
        String url = buildUrl(ENDPOINT_EXECUTAR_TRANSACAO);
        Transacao transacaoExecutada = restTemplate.postForObject(url, transacao, Transacao.class);

        return transacaoExecutada;
    }
}
