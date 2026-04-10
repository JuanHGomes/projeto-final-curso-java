package org.example.registrotransacaoservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceClientUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private TransacaoServiceClient client;

    @BeforeEach
    void setUp() {
        client = new TransacaoServiceClient(restTemplate);
        ReflectionTestUtils.setField(client, "transacaoServiceUrl", "http://localhost:8086");
    }

    @Test
    @DisplayName("Deve retornar saldo quando serviço responde com sucesso")
    void deveRetornarSaldoQuandoSucesso() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/saldo/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.ok(150000L));

        // when
        Long resultado = client.getSaldo(numeroConta);

        // then
        assertThat(resultado).isEqualTo(150000L);
    }

    @Test
    @DisplayName("Deve retornar zero quando serviço retorna erro HTTP")
    void deveRetornarZeroQuandoErroHttp() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/saldo/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // when
        Long resultado = client.getSaldo(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar zero quando serviço está indisponível")
    void deveRetornarZeroQuandoServicoIndisponivel() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/saldo/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenThrow(new RestClientException("Connection refused"));

        // when
        Long resultado = client.getSaldo(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar zero quando corpo da resposta é nulo")
    void deveRetornarZeroQuandoCorpoNulo() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/saldo/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.ok(null));

        // when
        Long resultado = client.getSaldo(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar limite de crédito quando serviço responde com sucesso")
    void deveRetornarLimiteCreditoQuandoSucesso() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/limiteCredito/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.ok(500000L));

        // when
        Long resultado = client.getLimiteCredito(numeroConta);

        // then
        assertThat(resultado).isEqualTo(500000L);
    }

    @Test
    @DisplayName("Deve retornar zero quando serviço retorna erro para limite")
    void deveRetornarZeroQuandoErroHttpParaLimite() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/limiteCredito/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        // when
        Long resultado = client.getLimiteCredito(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar zero quando exceção ao consultar limite")
    void deveRetornarZeroQuandoExcecaoParaLimite() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/limiteCredito/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenThrow(new RestClientException("Timeout"));

        // when
        Long resultado = client.getLimiteCredito(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("Deve retornar zero quando corpo do limite é nulo")
    void deveRetornarZeroQuandoCorpoLimiteNulo() {
        // given
        String numeroConta = "12345";
        String url = "http://localhost:8086/transacao/limiteCredito/12345";
        when(restTemplate.getForEntity(url, Long.class))
                .thenReturn(ResponseEntity.ok(null));

        // when
        Long resultado = client.getLimiteCredito(numeroConta);

        // then
        assertThat(resultado).isEqualTo(0L);
    }
}
