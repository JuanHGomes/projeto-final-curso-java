package org.example.transacaoservice.business.transacao;

import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.example.transacaoservice.business.validators.FraudeValidators;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoService Integration Tests")
class TransacaoServiceIntegrationTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private FraudeValidators fraudeValidator1;

    @Mock
    private FraudeValidators fraudeValidator2;

    @Mock
    private TransacaoOperators operator1;

    @Mock
    private TransacaoOperators operator2;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TransacaoService transacaoService;

    private Transacao transacaoDebito;
    private Transacao transacaoCredito;
    private List<FraudeValidators> fraudeValidatorsList;
    private List<TransacaoOperators> transacaoOperatorsList;

    @BeforeEach
    void setUp() {
        transacaoDebito = Transacao.builder()
                .numeroConta("123456")
                .valor(10000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Supermercado XYZ")
                .historico(new LinkedHashMap<>())
                .build();

        transacaoCredito = Transacao.builder()
                .numeroConta("123456")
                .valor(50000L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja ABC")
                .historico(new LinkedHashMap<>())
                .build();

        fraudeValidatorsList = List.of(fraudeValidator1, fraudeValidator2);
        transacaoOperatorsList = List.of(operator1, operator2);

        // Usar reflection para injetar as listas mockadas
        try {
            var fieldFraude = TransacaoService.class.getDeclaredField("fraudeValidatorsList");
            fieldFraude.setAccessible(true);
            fieldFraude.set(transacaoService, fraudeValidatorsList);

            var fieldOperators = TransacaoService.class.getDeclaredField("transacaoOperatorsList");
            fieldOperators.setAccessible(true);
            fieldOperators.set(transacaoService, transacaoOperatorsList);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar mocks via reflection", e);
        }

        // Configurar mock do redisTemplate (usando lenient para evitar erros em testes que não usam redis)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doNothing().when(valueOperations).set(anyString(), any());
        lenient().doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Nested
    @DisplayName("validarFundos - Validação de Fundos")
    class ValidarFundosTests {

        @Test
        @DisplayName("Deve validar fundos com sucesso para transação de débito")
        void deveValidarFundosSucessoDebito() throws Exception {
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            Transacao resultado = transacaoService.validarFundos(transacaoDebito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
            assertEquals(transacaoDebito.getNumeroConta(), resultado.getNumeroConta());
            assertEquals(transacaoDebito.getValor(), resultado.getValor());

            verify(contaRepository, times(1)).updateSaldo(transacaoDebito);
            verify(contaRepository, times(1)).setTransacaoToAguardanddoConfirmacao(transacaoDebito);
        }

        @Test
        @DisplayName("Deve validar fundos com sucesso para transação de crédito")
        void deveValidarFundosSucessoCredito() throws Exception {
            when(contaRepository.updateLimiteCredito(transacaoCredito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoCredito);

            Transacao resultado = transacaoService.validarFundos(transacaoCredito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
            assertEquals(transacaoCredito.getNumeroConta(), resultado.getNumeroConta());
            assertEquals(transacaoCredito.getValor(), resultado.getValor());

            verify(contaRepository, times(1)).updateLimiteCredito(transacaoCredito);
            verify(contaRepository, times(1)).setTransacaoToAguardanddoConfirmacao(transacaoCredito);
        }

        @Test
        @DisplayName("Deve retornar fundos insuficientes quando saldo não atualiza - débito")
        void deveRetornarFundosInsuficientesDebito() throws Exception {
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(false);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            Transacao resultado = transacaoService.validarFundos(transacaoDebito);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));

            verify(contaRepository, times(1)).updateSaldo(transacaoDebito);
        }

        @Test
        @DisplayName("Deve retornar fundos insuficientes quando limite não atualiza - crédito")
        void deveRetornarFundosInsuficientesCredito() throws Exception {
            when(contaRepository.updateLimiteCredito(transacaoCredito)).thenReturn(false);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoCredito);

            Transacao resultado = transacaoService.validarFundos(transacaoCredito);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));

            verify(contaRepository, times(1)).updateLimiteCredito(transacaoCredito);
        }

        @Test
        @DisplayName("Deve lançar exceção para tipo de transação inválido")
        void deveLancarExcecaoParaTipoInvalido() {
            Transacao transacaoInvalida = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(null)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Teste")
                    .historico(new LinkedHashMap<>())
                    .build();

            assertThrows(Exception.class, () -> transacaoService.validarFundos(transacaoInvalida));
        }

        @Test
        @DisplayName("Deve manter histórico previo e adicionar novo status")
        void deveManterHistoricoPrevio() throws Exception {
            transacaoDebito.getHistorico().put("VALIDACAO_CONTA", true);

            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            Transacao resultado = transacaoService.validarFundos(transacaoDebito);

            assertTrue(resultado.getHistorico().get("VALIDACAO_CONTA"));
            assertTrue(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
            assertEquals(2, resultado.getHistorico().size());
        }
    }

    @Nested
    @DisplayName("validarFraude - Validação de Fraude")
    class ValidarFraudeTests {

        @Test
        @DisplayName("Deve passar sem fraude quando todos validators retornam false")
        void devePassarSemFraude() {
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoDebito)).thenReturn(false);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertNotNull(resultado);
            assertFalse(resultado.getHistorico().get("FRAUDE"));

            verify(fraudeValidator1, times(1)).validate(transacaoDebito);
            verify(fraudeValidator2, times(1)).validate(transacaoDebito);
        }

        @Test
        @DisplayName("Deve detectar fraude quando primeiro validator retorna true")
        void deveDetectarFraudePrimeiroValidator() {
            lenient().when(fraudeValidator1.validate(transacaoDebito)).thenReturn(true);
            lenient().when(fraudeValidator2.validate(transacaoDebito)).thenReturn(false);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FRAUDE"));
        }

        @Test
        @DisplayName("Deve detectar fraude quando segundo validator retorna true")
        void deveDetectarFraudeSegundoValidator() {
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoDebito)).thenReturn(true);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FRAUDE"));
        }

        @Test
        @DisplayName("Deve detectar fraude quando ambos validators retornam true")
        void deveDetectarFraudeAmbosValidators() {
            lenient().when(fraudeValidator1.validate(transacaoDebito)).thenReturn(true);
            lenient().when(fraudeValidator2.validate(transacaoDebito)).thenReturn(true);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("FRAUDE"));
        }

        @Test
        @DisplayName("Deve parar validação no primeiro validator que detectar fraude (short-circuit)")
        void devePararValidacaoNoPrimeiroFraude() {
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(true);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertTrue(resultado.getHistorico().get("FRAUDE"));
            verify(fraudeValidator1, times(1)).validate(transacaoDebito);
            // O segundo validator NÃO deve ser chamado devido ao short-circuit do anyMatch()
            verify(fraudeValidator2, never()).validate(transacaoDebito);
        }

        @Test
        @DisplayName("Deve manter histórico previo e adicionar status de fraude")
        void deveManterHistoricoPrevioComFraude() {
            transacaoDebito.getHistorico().put("VALIDACAO_CONTA", true);
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(true);

            Transacao resultado = transacaoService.validarFraude(transacaoDebito);

            assertTrue(resultado.getHistorico().get("VALIDACAO_CONTA"));
            assertTrue(resultado.getHistorico().get("FRAUDE"));
            assertEquals(2, resultado.getHistorico().size());
        }

        @Test
        @DisplayName("Deve processar todos validators quando nenhum detecta fraude")
        void deveProcessarTodosValidatorsSemFraude() {
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoDebito)).thenReturn(false);

            transacaoService.validarFraude(transacaoDebito);

            verify(fraudeValidator1, times(1)).validate(transacaoDebito);
            verify(fraudeValidator2, times(1)).validate(transacaoDebito);
        }
    }

    @Nested
    @DisplayName("executarTransacao - Execução de Transação")
    class ExecutarTransacaoTests {

        @Test
        @DisplayName("Deve confirmar transação em todos os operators")
        void deveConfirmarTransacaoEmTodosOperators() {
            doNothing().when(operator1).confirmarTransacao(transacaoDebito);
            doNothing().when(operator2).confirmarTransacao(transacaoDebito);

            Transacao resultado = transacaoService.executarTransacao(transacaoDebito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("EXECUCAO_SUCESSO"));

            verify(operator1, times(1)).confirmarTransacao(transacaoDebito);
            verify(operator2, times(1)).confirmarTransacao(transacaoDebito);
        }

        @Test
        @DisplayName("Deve manter histórico previo e adicionar status de execução")
        void deveManterHistoricoPrevioComExecucao() {
            transacaoDebito.getHistorico().put("FUNDOS_SUFICIENTES", true);
            doNothing().when(operator1).confirmarTransacao(transacaoDebito);
            doNothing().when(operator2).confirmarTransacao(transacaoDebito);

            Transacao resultado = transacaoService.executarTransacao(transacaoDebito);

            assertTrue(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));
            assertTrue(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
            assertEquals(2, resultado.getHistorico().size());
        }

        @Test
        @DisplayName("Deve executar transação de crédito")
        void deveExecutarTransacaoCredito() {
            doNothing().when(operator1).confirmarTransacao(transacaoCredito);
            doNothing().when(operator2).confirmarTransacao(transacaoCredito);

            Transacao resultado = transacaoService.executarTransacao(transacaoCredito);

            assertNotNull(resultado);
            assertTrue(resultado.getHistorico().get("EXECUCAO_SUCESSO"));
            assertEquals(transacaoCredito.getTipoTransacao(), resultado.getTipoTransacao());
        }
    }

    @Nested
    @DisplayName("estornarTransacao - Estorno de Transação")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transação em todos os operators")
        void deveEstornarTransacaoEmTodosOperators() {
            doNothing().when(operator1).estornarTransacao(transacaoDebito);
            doNothing().when(operator2).estornarTransacao(transacaoDebito);

            transacaoService.estornarTransacao(transacaoDebito);

            verify(operator1, times(1)).estornarTransacao(transacaoDebito);
            verify(operator2, times(1)).estornarTransacao(transacaoDebito);
        }

        @Test
        @DisplayName("Deve estornar transação de crédito")
        void deveEstornarTransacaoCredito() {
            doNothing().when(operator1).estornarTransacao(transacaoCredito);
            doNothing().when(operator2).estornarTransacao(transacaoCredito);

            transacaoService.estornarTransacao(transacaoCredito);

            verify(operator1, times(1)).estornarTransacao(transacaoCredito);
            verify(operator2, times(1)).estornarTransacao(transacaoCredito);
        }

        @Test
        @DisplayName("Deve estornar mesmo quando um operator lança exceção")
        void deveEstornarMesmoComExcecao() {
            doThrow(new RuntimeException("Erro no operator 1")).when(operator1).estornarTransacao(transacaoDebito);
            lenient().doNothing().when(operator2).estornarTransacao(transacaoDebito);

            assertThrows(RuntimeException.class, () -> transacaoService.estornarTransacao(transacaoDebito));

            verify(operator1, times(1)).estornarTransacao(transacaoDebito);
            // O segundo operator NÃO é chamado devido à exceção
            verify(operator2, never()).estornarTransacao(transacaoDebito);
        }
    }

    @Nested
    @DisplayName("Cenários de Integração Completa")
    class CenariosIntegracaoTests {

        @Test
        @DisplayName("Fluxo completo: validarFundos -> validarFraude -> executarTransacao com sucesso")
        void fluxoCompletoComSucesso() throws Exception {
            // Setup débito
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoDebito)).thenReturn(false);
            doNothing().when(operator1).confirmarTransacao(any());
            doNothing().when(operator2).confirmarTransacao(any());

            // Passo 1: Validar fundos
            Transacao resultado1 = transacaoService.validarFundos(transacaoDebito);
            assertTrue(resultado1.getHistorico().get("FUNDOS_SUFICIENTES"));

            // Passo 2: Validar fraude
            Transacao resultado2 = transacaoService.validarFraude(resultado1);
            assertFalse(resultado2.getHistorico().get("FRAUDE"));

            // Passo 3: Executar transação
            Transacao resultado3 = transacaoService.executarTransacao(resultado2);
            assertTrue(resultado3.getHistorico().get("EXECUCAO_SUCESSO"));

            // Verificar histórico completo
            assertEquals(3, resultado3.getHistorico().size());
            assertTrue(resultado3.getHistorico().get("FUNDOS_SUFICIENTES"));
            assertFalse(resultado3.getHistorico().get("FRAUDE"));
            assertTrue(resultado3.getHistorico().get("EXECUCAO_SUCESSO"));
        }

        @Test
        @DisplayName("Fluxo com fraude: validarFundos -> validarFraude detecta fraude")
        void fluxoComFraudeDetectada() throws Exception {
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(true);

            Transacao resultado1 = transacaoService.validarFundos(transacaoDebito);
            assertTrue(resultado1.getHistorico().get("FUNDOS_SUFICIENTES"));

            Transacao resultado2 = transacaoService.validarFraude(resultado1);
            assertTrue(resultado2.getHistorico().get("FRAUDE"));

            // Transação fraudulenta NÃO deve ser executada
            verify(operator1, never()).confirmarTransacao(any());
            verify(operator2, never()).confirmarTransacao(any());
        }

        @Test
        @DisplayName("Fluxo com fundos insuficientes: validarFundos retorna false")
        void fluxoComFundosInsuficientes() throws Exception {
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(false);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            Transacao resultado = transacaoService.validarFundos(transacaoDebito);
            assertFalse(resultado.getHistorico().get("FUNDOS_SUFICIENTES"));

            // Com fundos insuficientes, fraude e execução não devem prosseguir
            verify(fraudeValidator1, never()).validate(any());
            verify(operator1, never()).confirmarTransacao(any());
        }

        @Test
        @DisplayName("Fluxo completo para transação de crédito")
        void fluxoCompletoCredito() throws Exception {
            when(contaRepository.updateLimiteCredito(transacaoCredito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoCredito);
            when(fraudeValidator1.validate(transacaoCredito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoCredito)).thenReturn(false);
            doNothing().when(operator1).confirmarTransacao(any());
            doNothing().when(operator2).confirmarTransacao(any());

            Transacao resultado1 = transacaoService.validarFundos(transacaoCredito);
            assertTrue(resultado1.getHistorico().get("FUNDOS_SUFICIENTES"));

            Transacao resultado2 = transacaoService.validarFraude(resultado1);
            assertFalse(resultado2.getHistorico().get("FRAUDE"));

            Transacao resultado3 = transacaoService.executarTransacao(resultado2);
            assertTrue(resultado3.getHistorico().get("EXECUCAO_SUCESSO"));

            verify(contaRepository, times(1)).updateLimiteCredito(transacaoCredito);
            verify(contaRepository, never()).updateSaldo(any());
        }

        @Test
        @DisplayName("Fluxo com estorno: executarTransacao -> estornarTransacao")
        void fluxoComEstorno() throws Exception {
            when(contaRepository.updateSaldo(transacaoDebito)).thenReturn(true);
            doNothing().when(contaRepository).setTransacaoToAguardanddoConfirmacao(transacaoDebito);
            when(fraudeValidator1.validate(transacaoDebito)).thenReturn(false);
            when(fraudeValidator2.validate(transacaoDebito)).thenReturn(false);
            doNothing().when(operator1).confirmarTransacao(any());
            doNothing().when(operator2).confirmarTransacao(any());
            doNothing().when(operator1).estornarTransacao(any());
            doNothing().when(operator2).estornarTransacao(any());

            // Fluxo normal
            Transacao resultado = transacaoService.validarFundos(transacaoDebito);
            resultado = transacaoService.validarFraude(resultado);
            resultado = transacaoService.executarTransacao(resultado);

            // Estorno
            transacaoService.estornarTransacao(resultado);

            verify(operator1, times(1)).estornarTransacao(resultado);
            verify(operator2, times(1)).estornarTransacao(resultado);
        }
    }
}
