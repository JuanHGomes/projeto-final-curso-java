package com.example.workflow.business;

import com.example.workflow.business.model.AtualizacaoHistorico;
import com.example.workflow.business.model.Notificacao;
import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.data.TransacaoRepository;
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
@DisplayName("Testes Unitários - TransacaoService")
class TransacaoServiceUnitTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private TransacaoProducer kafkaProducer;

    @InjectMocks
    private TransacaoService transacaoService;

    private Transacao transacaoValida;
    private static final String TOPICO_NOTIFICACAO = "notificacaoProducer-out-0";

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
        @DisplayName("Deve validar fundos com sucesso")
        void deveValidarFundosComSucesso() {
            when(transacaoRepository.validarFundos(transacaoValida)).thenReturn(transacaoValida);

            Transacao resultado = transacaoService.validarFundos(transacaoValida);

            assertNotNull(resultado);
            assertEquals(transacaoValida.getNumeroConta(), resultado.getNumeroConta());
            verify(transacaoRepository, times(1)).validarFundos(transacaoValida);
        }

        @Test
        @DisplayName("Deve retornar transacao com fundos insuficientes")
        void deveRetornarFundosInsuficientes() {
            LinkedHashMap<String, Boolean> historicoNegado = new LinkedHashMap<>();
            historicoNegado.put("FUNDOS_SUFICIENTES", false);

            Transacao transacaoFundosInsuficientes = Transacao.builder()
                    .numeroConta("123456")
                    .valor(999999L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historicoNegado)
                    .build();

            when(transacaoRepository.validarFundos(transacaoValida)).thenReturn(transacaoFundosInsuficientes);

            Transacao resultado = transacaoService.validarFundos(transacaoValida);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
        }
    }

    @Nested
    @DisplayName("Validar Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve validar transacao sem fraude")
        void deveValidarSemFraude() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", false);

            Transacao transacaoSemFraude = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(transacaoRepository.validarFraude(transacaoValida)).thenReturn(transacaoSemFraude);

            Transacao resultado = transacaoService.validarFraude(transacaoValida);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FRAUDE"));
        }

        @Test
        @DisplayName("Deve detectar fraude na transacao")
        void deveDetectarFraude() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", true);

            Transacao transacaoComFraude = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(transacaoRepository.validarFraude(transacaoValida)).thenReturn(transacaoComFraude);

            Transacao resultado = transacaoService.validarFraude(transacaoValida);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FRAUDE"));
        }
    }

    @Nested
    @DisplayName("Executar Transacao")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve executar transacao com sucesso")
        void deveExecutarTransacaoComSucesso() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", true);

            Transacao transacaoExecutada = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(transacaoRepository.executarTransacao(transacaoValida)).thenReturn(transacaoExecutada);

            Transacao resultado = transacaoService.executarTransacao(transacaoValida);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
        }

        @Test
        @DisplayName("Deve falhar ao executar transacao")
        void deveFalharAoExecutarTransacao() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", false);

            Transacao transacaoFalha = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(transacaoRepository.executarTransacao(transacaoValida)).thenReturn(transacaoFalha);

            Transacao resultado = transacaoService.executarTransacao(transacaoValida);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
        }
    }

    @Nested
    @DisplayName("Estornar Transacao")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transacao com sucesso")
        void deveEstornarTransacaoComSucesso() {
            doNothing().when(transacaoRepository).estornarTransacao(any(Transacao.class));

            transacaoService.estornarTransacao(transacaoValida);

            verify(transacaoRepository, times(1)).estornarTransacao(transacaoValida);
        }
    }

    @Nested
    @DisplayName("Enviar Notificação")
    class EnviarNotificacaoTests {

        @Test
        @DisplayName("Deve enviar notificacao de fundos insuficientes")
        void deveEnviarNotificacaoFundosInsuficientes() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FUNDOS_SUFICIENTES", false);

            Transacao transacao = Transacao.builder()
                    .numeroConta("123456")
                    .valor(999999L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja Teste")
                    .historico(historico)
                    .build();

            when(kafkaProducer.sendMessage(anyString(), any(Notificacao.class))).thenReturn(true);

            transacaoService.enviarNotificacao(transacao);

            verify(kafkaProducer, times(1)).sendMessage(eq(TOPICO_NOTIFICACAO), any(Notificacao.class));
        }

        @Test
        @DisplayName("Deve enviar notificacao de fraude detectada")
        void deveEnviarNotificacaoFraude() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", true);

            Transacao transacao = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(kafkaProducer.sendMessage(anyString(), any(Notificacao.class))).thenReturn(true);

            transacaoService.enviarNotificacao(transacao);

            verify(kafkaProducer, times(1)).sendMessage(eq(TOPICO_NOTIFICACAO), any(Notificacao.class));
        }

        @Test
        @DisplayName("Deve enviar notificacao de transacao concluida com sucesso")
        void deveEnviarNotificacaoSucesso() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", true);

            Transacao transacao = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            when(kafkaProducer.sendMessage(anyString(), any(Notificacao.class))).thenReturn(true);

            transacaoService.enviarNotificacao(transacao);

            verify(kafkaProducer, times(1)).sendMessage(eq(TOPICO_NOTIFICACAO), any(Notificacao.class));
        }

        @Test
        @DisplayName("Deve lancar excecao para etapa desconhecida")
        void deveLancarExcecaoParaEtapaDesconhecida() {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("ETAPA_DESCONHECIDA", true);

            Transacao transacao = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            assertThrows(RuntimeException.class, () -> transacaoService.enviarNotificacao(transacao));
            verify(kafkaProducer, never()).sendMessage(anyString(), any(Notificacao.class));
        }
    }

    @Nested
    @DisplayName("Enviar Transacao para Registro")
    class EnviarTransacaoParaRegistroTests {

        @Test
        @DisplayName("Deve enviar transacao para registro com sucesso")
        void deveEnviarTransacaoParaRegistroComSucesso() {
            when(transacaoRepository.enviarTransacaoParaRegistro(transacaoValida)).thenReturn(true);

            boolean resultado = transacaoService.enviarTransacaoParaRegistro(transacaoValida);

            assertTrue(resultado);
            verify(transacaoRepository, times(1)).enviarTransacaoParaRegistro(transacaoValida);
        }

        @Test
        @DisplayName("Deve falhar ao enviar transacao para registro")
        void deveFalharAoEnviarTransacaoParaRegistro() {
            when(transacaoRepository.enviarTransacaoParaRegistro(transacaoValida)).thenReturn(false);

            boolean resultado = transacaoService.enviarTransacaoParaRegistro(transacaoValida);

            assertFalse(resultado);
        }
    }
}
