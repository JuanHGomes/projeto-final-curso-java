package org.example.registrotransacaoservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransacaoServiceClient {

    private final RestTemplate restTemplate;

    @Value("${transacao.service.url:http://localhost:8086}")
    private String transacaoServiceUrl;

    public Long getSaldo(String numeroConta) {
        String url = transacaoServiceUrl + "/transacao/saldo/" + numeroConta;

        try {
            ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Não foi possível obter saldo para conta {}: status {}", numeroConta, response.getStatusCode());
            return 0L;
        } catch (RestClientException e) {
            log.error("Erro ao consultar saldo do transacao-service para conta {}: {}", numeroConta, e.getMessage());
            return 0L;
        }
    }

    public Long getLimiteCredito(String numeroConta) {
        String url = transacaoServiceUrl + "/transacao/limiteCredito/" + numeroConta;

        try {
            ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Não foi possível obter limite de crédito para conta {}: status {}", numeroConta, response.getStatusCode());
            return 0L;
        } catch (RestClientException e) {
            log.error("Erro ao consultar limite de crédito do transacao-service para conta {}: {}", numeroConta, e.getMessage());
            return 0L;
        }
    }
}
