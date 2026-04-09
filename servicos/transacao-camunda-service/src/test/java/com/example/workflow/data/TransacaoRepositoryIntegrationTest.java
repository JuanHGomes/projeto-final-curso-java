package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.messaging.TransacaoProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração - TransacaoRepository")
class TransacaoRepositoryIntegrationTest {

    @Mock
    private TransacaoRestClient transacaoRestClient;

    @Mock
    private TransacaoProducer producer;

    @InjectMocks
    private TransacaoRepository transacaoRepository;

    private Transacao transacaoValida;
    private static final String TOPICO_REGISTRO = "registroTransacaoProducer-out-0";

    @BeforeEach
    void setUp() {
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("FUNDOS_SUFICIENTES", true);
        historico.put("FRAUDE", false);
        historico.put("EXECUCAO_SUCESSO", true);

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
        @DisplayName("Deve validar fundos via REST client com sucesso")
        void deveValidarFundosViaRestClient() {
            // Arrange
            when(transacaoRestClient.validarFundos(transacaoValida)).thenReturn(transacaoValida);

            // Act
            Transacao resultado = transacaoRepository.validarFundos(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
            assertEquals(transacaoValida.getValor(), resultado.getValor());
            verify(transacaoRestClient, times(1)).validarFundos(transacaoValida);
        }

        @Test
        @DisplayName("Deve retornar transacao com fundos insuficientes via REST")
        void deveRetornarFundosInsuficientesViaRest() {
            // Arrange
            LinkedHashMap<String, Boolean> historicoNegado = new LinkedHashMap<>();
            historicoNegado.put("FUNDOS_SUFICIENTES", false);
            
            Transacao transacaoFundosInsuficientes = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.COMPRA)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historicoNegado)
                    .build();

            when(transacaoRestClient.validarFundos(transacaoValida)).thenReturn(transacaoFundosInsuficientes);

            // Act
            Transacao resultado = transacaoRepository.validarFundos(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
            verify(transacaoRestClient, times(1)).validarFundos(transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando REST client falhar")
        void deveLancarExcecaoQuandoRestClientFalhar() {
            // Arrange
            when(transacaoRestClient.validarFundos(transacaoValida))
                    .thenThrow(new RuntimeException("Erro de conexão"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRepository.validarFundos(transacaoValida));
            verify(transacaoRestClient, times(1)).validarFundos(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Validar Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve validar fraude via REST client sem detectar fraude")
        void deveValidarFraudeSemDetectar() {
            // Arrange
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", false);
            
            Transacao transacaoSemFraude = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.COMPRA)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historico)
                    .build();

            when(transacaoRestClient.validarFraude(transacaoValida)).thenReturn(transacaoSemFraude);

            // Act
            Transacao resultado = transacaoRepository.validarFraude(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FRAUDE"));
            verify(transacaoRestClient, times(1)).validarFraude(transacaoValida);
        }

        @Test
        @DisplayName("Deve validar fraude via REST client detectando fraude")
        void deveValidarFraudeDetectando() {
            // Arrange
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", true);
            
            Transacao transacaoComFraude = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.COMPRA)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Suspeita")
                    .historico(historico)
                    .build();

            when(transacaoRestClient.validarFraude(transacaoValida)).thenReturn(transacaoComFraude);

            // Act
            Transacao resultado = transacaoRepository.validarFraude(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FRAUDE"));
            verify(transacaoRestClient, times(1)).validarFraude(transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando validacao de fraude falhar")
        void deveLancarExcecaoQuandoValidacaoFraudeFalhar() {
            // Arrange
            when(transacaoRestClient.validarFraude(transacaoValida))
                    .thenThrow(new RuntimeException("Erro na validação de fraude"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRepository.validarFraude(transacaoValida));
            verify(transacaoRestClient, times(1)).validarFraude(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Executar Transacao")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve executar transacao via REST client com sucesso")
        void deveExecutarTransacaoViaRestClient() {
            // Arrange
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", true);
            
            Transacao transacaoExecutada = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.COMPRA)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historico)
                    .build();

            when(transacaoRestClient.executarTransacao(transacaoValida)).thenReturn(transacaoExecutada);

            // Act
            Transacao resultado = transacaoRepository.executarTransacao(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
            verify(transacaoRestClient, times(1)).executarTransacao(transacaoValida);
        }

        @Test
        @DisplayName("Deve executar transacao via REST client com falha")
        void deveExecutarTransacaoViaRestClientComFalha() {
            // Arrange
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", false);
            
            Transacao transacaoFalha = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.COMPRA)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historico)
                    .build();

            when(transacaoRestClient.executarTransacao(transacaoValida)).thenReturn(transacaoFalha);

            // Act
            Transacao resultado = transacaoRepository.executarTransacao(transacaoValida);

            // Assert
            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
            verify(transacaoRestClient, times(1)).executarTransacao(transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando execucao falhar")
        void deveLancarExcecaoQuandoExecucaoFalhar() {
            // Arrange
            when(transacaoRestClient.executarTransacao(transacaoValida))
                    .thenThrow(new RuntimeException("Erro na execução"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRepository.executarTransacao(transacaoValida));
            verify(transacaoRestClient, times(1)).executarTransacao(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Estornar Transacao")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transacao via REST client com sucesso")
        void deveEstornarTransacaoViaRestClient() {
            // Arrange
            when(transacaoRestClient.estornarTransacao(transacaoValida)).thenReturn(null);

            // Act
            transacaoRepository.estornarTransacao(transacaoValida);

            // Assert
            verify(transacaoRestClient, times(1)).estornarTransacao(transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando estorno falhar")
        void deveLancarExcecaoQuandoEstornoFalhar() {
            // Arrange
            doThrow(new RuntimeException("Erro no estorno")).when(transacaoRestClient).estornarTransacao(transacaoValida);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> transacaoRepository.estornarTransacao(transacaoValida));
            verify(transacaoRestClient, times(1)).estornarTransacao(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Enviar Transacao para Registro")
    class EnviarTransacaoParaRegistroTests {

        @Test
        @DisplayName("Deve enviar transacao para registro via Kafka com sucesso")
        void deveEnviarTransacaoParaRegistroViaKafka() {
            // Arrange
            when(producer.sendMessage(eq(TOPICO_REGISTRO), any(Transacao.class))).thenReturn(true);

            // Act
            boolean resultado = transacaoRepository.enviarTransacaoParaRegistro(transacaoValida);

            // Assert
            assertTrue(resultado);
            verify(producer, times(1)).sendMessage(TOPICO_REGISTRO, transacaoValida);
        }

        @Test
        @DisplayName("Deve falhar ao enviar transacao para registro via Kafka")
        void deveFalharAoEnviarTransacaoParaRegistroViaKafka() {
            // Arrange
            when(producer.sendMessage(eq(TOPICO_REGISTRO), any(Transacao.class))).thenReturn(false);

            // Act
            boolean resultado = transacaoRepository.enviarTransacaoParaRegistro(transacaoValida);

            // Assert
            assertFalse(resultado);
            verify(producer, times(1)).sendMessage(TOPICO_REGISTRO, transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando Kafka producer falhar")
        void deveLancarExcecaoQuandoKafkaProducerFalhar() {
            // Arrange
            when(producer.sendMessage(eq(TOPICO_REGISTRO), any(Transacao.class)))
                    .thenThrow(new RuntimeException("Erro no Kafka"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                    () -> transacaoRepository.enviarTransacaoParaRegistro(transacaoValida));
            verify(producer, times(1)).sendMessage(TOPICO_REGISTRO, transacaoValida);
        }
    }
}
