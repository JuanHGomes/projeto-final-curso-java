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
@DisplayName("Testes Unitários - TransacaoRepository")
class TransacaoRepositoryUnitTest {

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
        @DisplayName("Deve validar fundos via REST client")
        void deveValidarFundosViaRestClient() {
            when(transacaoRestClient.validarFundos(transacaoValida)).thenReturn(transacaoValida);

            Transacao resultado = transacaoRepository.validarFundos(transacaoValida);

            assertNotNull(resultado);
            verify(transacaoRestClient).validarFundos(transacaoValida);
        }

        @Test
        @DisplayName("Deve lancar excecao quando REST client falhar")
        void deveLancarExcecaoQuandoRestClientFalhar() {
            when(transacaoRestClient.validarFundos(transacaoValida))
                    .thenThrow(new RuntimeException("Erro de conexão"));

            assertThrows(RuntimeException.class, () -> transacaoRepository.validarFundos(transacaoValida));
        }
    }

    @Nested
    @DisplayName("Validar Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve validar fraude via REST client")
        void deveValidarFraudeViaRestClient() {
            when(transacaoRestClient.validarFraude(transacaoValida)).thenReturn(transacaoValida);

            Transacao resultado = transacaoRepository.validarFraude(transacaoValida);

            assertNotNull(resultado);
            verify(transacaoRestClient).validarFraude(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Executar Transacao")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve executar transacao via REST client")
        void deveExecutarTransacaoViaRestClient() {
            when(transacaoRestClient.executarTransacao(transacaoValida)).thenReturn(transacaoValida);

            Transacao resultado = transacaoRepository.executarTransacao(transacaoValida);

            assertNotNull(resultado);
        }
    }

    @Nested
    @DisplayName("Estornar Transacao")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transacao via REST client")
        void deveEstornarTransacaoViaRestClient() {
            doNothing().when(transacaoRestClient).estornarTransacao(transacaoValida);

            transacaoRepository.estornarTransacao(transacaoValida);

            verify(transacaoRestClient).estornarTransacao(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Enviar Transacao para Registro")
    class EnviarTransacaoParaRegistroTests {

        @Test
        @DisplayName("Deve enviar transacao para registro via Kafka com sucesso")
        void deveEnviarTransacaoParaRegistroViaKafka() {
            when(producer.sendMessage(eq(TOPICO_REGISTRO), any(Transacao.class))).thenReturn(true);

            boolean resultado = transacaoRepository.enviarTransacaoParaRegistro(transacaoValida);

            assertTrue(resultado);
            verify(producer).sendMessage(TOPICO_REGISTRO, transacaoValida);
        }

        @Test
        @DisplayName("Deve falhar ao enviar transacao para registro via Kafka")
        void deveFalharAoEnviarTransacaoParaRegistroViaKafka() {
            when(producer.sendMessage(eq(TOPICO_REGISTRO), any(Transacao.class))).thenReturn(false);

            boolean resultado = transacaoRepository.enviarTransacaoParaRegistro(transacaoValida);

            assertFalse(resultado);
        }
    }
}
