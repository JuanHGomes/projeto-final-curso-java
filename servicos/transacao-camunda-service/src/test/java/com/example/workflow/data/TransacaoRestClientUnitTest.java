package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TransacaoRestClient")
class TransacaoRestClientUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private TransacaoRestClient transacaoRestClient;
    private Transacao transacaoValida;

    private static final String BASE_URL = "http://localhost:8086/transacao/";

    @BeforeEach
    void setUp() {
        transacaoRestClient = new TransacaoRestClient(restTemplate);

        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("FUNDOS_SUFICIENTES", true);

        transacaoValida = Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja Teste")
                .historico(historico)
                .build();
    }

    @Nested
    @DisplayName("Validar Fundos")
    class ValidarFundosTests {

        @Test
        @DisplayName("Deve validar fundos com sucesso via REST")
        void deveValidarFundosComSucesso() {
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            Transacao resultado = transacaoRestClient.validarFundos(transacaoValida);

            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
        }

        @Test
        @DisplayName("Deve retornar null quando validacao de fundos falhar")
        void deveRetornarNullQuandoValidacaoFalhar() {
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            Transacao resultado = transacaoRestClient.validarFundos(transacaoValida);

            assertNull(resultado);
        }

        @Test
        @DisplayName("Deve lancar excecao quando REST estiver indisponivel")
        void deveLancarExcecaoQuandoRestIndisponivel() {
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThrows(RuntimeException.class, () -> transacaoRestClient.validarFundos(transacaoValida));
        }
    }

    @Nested
    @DisplayName("Validar Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve validar fraude com sucesso via REST")
        void deveValidarFraudeComSucesso() {
            String url = BASE_URL + "validarFraude";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            Transacao resultado = transacaoRestClient.validarFraude(transacaoValida);

            assertNotNull(resultado);
            verify(restTemplate).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve retornar null quando validacao de fraude falhar")
        void deveRetornarNullQuandoValidacaoFraudeFalhar() {
            String url = BASE_URL + "validarFraude";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            assertNull(transacaoRestClient.validarFraude(transacaoValida));
        }
    }

    @Nested
    @DisplayName("Executar Transacao")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve executar transacao com sucesso via REST")
        void deveExecutarTransacaoComSucesso() {
            String url = BASE_URL + "executarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            Transacao resultado = transacaoRestClient.executarTransacao(transacaoValida);

            assertNotNull(resultado);
        }

        @Test
        @DisplayName("Deve retornar null quando execucao falhar")
        void deveRetornarNullQuandoExecucaoFalhar() {
            String url = BASE_URL + "executarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            assertNull(transacaoRestClient.executarTransacao(transacaoValida));
        }
    }

    @Nested
    @DisplayName("Estornar Transacao")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transacao com sucesso via REST")
        void deveEstornarTransacaoComSucesso() {
            String url = BASE_URL + "estornarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Void.class)))
                    .thenReturn(null);

            transacaoRestClient.estornarTransacao(transacaoValida);

            verify(restTemplate).postForObject(eq(url), any(Transacao.class), eq(Void.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando estorno estiver indisponivel")
        void deveLancarExcecaoQuandoEstornoIndisponivel() {
            String url = BASE_URL + "estornarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Void.class)))
                    .thenThrow(new RuntimeException("Service error"));

            assertThrows(RuntimeException.class, () -> transacaoRestClient.estornarTransacao(transacaoValida));
        }
    }
}
