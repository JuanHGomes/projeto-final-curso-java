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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração - TransacaoRestClient")
class TransacaoRestClientIntegrationTest {

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
                .tipoTransacao(TipoTransacao.COMPRA)
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
            // Arrange
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            // Act
            Transacao resultado = transacaoRestClient.validarFundos(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve retornar null quando validacao de fundos falhar")
        void deveRetornarNullQuandoValidacaoFalhar() {
            // Arrange
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            // Act
            Transacao resultado = transacaoRestClient.validarFundos(transacaoValida);

            // Assert
            assertNull(resultado);
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando REST estiver indisponivel")
        void deveLancarExcecaoQuandoRestIndisponivel() {
            // Arrange
            String url = BASE_URL + "validarFundos";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRestClient.validarFundos(transacaoValida));
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }
    }

    @Nested
    @DisplayName("Validar Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve validar fraude com sucesso via REST")
        void deveValidarFraudeComSucesso() {
            // Arrange
            String url = BASE_URL + "validarFraude";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            // Act
            Transacao resultado = transacaoRestClient.validarFraude(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve retornar null quando validacao de fraude falhar")
        void deveRetornarNullQuandoValidacaoFraudeFalhar() {
            // Arrange
            String url = BASE_URL + "validarFraude";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            // Act
            Transacao resultado = transacaoRestClient.validarFraude(transacaoValida);

            // Assert
            assertNull(resultado);
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando validacao de fraude estiver indisponivel")
        void deveLancarExcecaoQuandoValidacaoFraudeIndisponivel() {
            // Arrange
            String url = BASE_URL + "validarFraude";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenThrow(new RuntimeException("Service unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRestClient.validarFraude(transacaoValida));
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }
    }

    @Nested
    @DisplayName("Executar Transacao")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve executar transacao com sucesso via REST")
        void deveExecutarTransacaoComSucesso() {
            // Arrange
            String url = BASE_URL + "executarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(transacaoValida);

            // Act
            Transacao resultado = transacaoRestClient.executarTransacao(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve retornar null quando execucao falhar")
        void deveRetornarNullQuandoExecucaoFalhar() {
            // Arrange
            String url = BASE_URL + "executarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenReturn(null);

            // Act
            Transacao resultado = transacaoRestClient.executarTransacao(transacaoValida);

            // Assert
            assertNull(resultado);
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando execucao estiver indisponivel")
        void deveLancarExcecaoQuandoExecucaoIndisponivel() {
            // Arrange
            String url = BASE_URL + "executarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Transacao.class)))
                    .thenThrow(new RuntimeException("Timeout"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRestClient.executarTransacao(transacaoValida));
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Transacao.class));
        }
    }

    @Nested
    @DisplayName("Estornar Transacao")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transacao com sucesso via REST")
        void deveEstornarTransacaoComSucesso() {
            // Arrange
            String url = BASE_URL + "estornarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Void.class)))
                    .thenReturn(null);

            // Act
            transacaoRestClient.estornarTransacao(transacaoValida);

            // Assert
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Void.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando estorno estiver indisponivel")
        void deveLancarExcecaoQuandoEstornoIndisponivel() {
            // Arrange
            String url = BASE_URL + "estornarTransacao";
            when(restTemplate.postForObject(eq(url), any(Transacao.class), eq(Void.class)))
                    .thenThrow(new RuntimeException("Service error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRestClient.estornarTransacao(transacaoValida));
            verify(restTemplate, times(1)).postForObject(eq(url), any(Transacao.class), eq(Void.class));
        }
    }
}
