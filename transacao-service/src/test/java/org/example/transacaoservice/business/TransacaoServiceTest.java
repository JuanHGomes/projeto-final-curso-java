package org.example.transacaoservice.business;

import org.example.transacaoservice.business.transacao.TransacaoService;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.validators.FraudeValidators;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoService Tests")
class TransacaoServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private FraudeValidators fraudeValidator1;

    @Mock
    private FraudeValidators fraudeValidator2;

    @InjectMocks
    private TransacaoService transacaoService;

    private Transacao transacao;

    @BeforeEach
    void setUp() {
        transacao = new Transacao();
        transacao.setNumeroConta("123456");
        transacao.setValor(100L);
    }

    // ========== validarFundos - DEBITO ==========

    @Test
    @DisplayName("Should return true when validating debit with sufficient balance")
    void deveRetornarTrueAoValidarDebitoComSaldoSuficiente() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.DEBITO);
        when(contaRepository.getSaldoByNumeroConta("123456")).thenReturn(150L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertTrue(resultado);
        verify(contaRepository, times(1)).getSaldoByNumeroConta("123456");
    }

    @Test
    @DisplayName("Should return false when validating debit with insufficient balance")
    void deveRetornarFalseAoValidarDebitoComSaldoInsuficiente() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.DEBITO);
        when(contaRepository.getSaldoByNumeroConta("123456")).thenReturn(50L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertFalse(resultado);
        verify(contaRepository, times(1)).getSaldoByNumeroConta("123456");
    }

    @Test
    @DisplayName("Should return true when debit value equals balance")
    void deveRetornarTrueQuandoDebitoIgualAoSaldo() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.DEBITO);
        transacao.setValor(100L);
        when(contaRepository.getSaldoByNumeroConta("123456")).thenReturn(100L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Should return false when debit value exceeds balance by 1")
    void deveRetornarFalseQuandoDebitoExcedeSaldoEm1() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.DEBITO);
        when(contaRepository.getSaldoByNumeroConta("123456")).thenReturn(99L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertFalse(resultado);
    }

    // ========== validarFundos - CREDITO ==========

    @Test
    @DisplayName("Should return true when validating credit with sufficient limit")
    void deveRetornarTrueAoValidarCreditoComLimiteSuficiente() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.CREDITO);
        when(contaRepository.getLimiteCreditoByNumeroConta("123456")).thenReturn(200L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertTrue(resultado);
        verify(contaRepository, times(1)).getLimiteCreditoByNumeroConta("123456");
    }

    @Test
    @DisplayName("Should return false when validating credit with insufficient limit")
    void deveRetornarFalseAoValidarCreditoComLimiteInsuficiente() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.CREDITO);
        when(contaRepository.getLimiteCreditoByNumeroConta("123456")).thenReturn(50L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertFalse(resultado);
        verify(contaRepository, times(1)).getLimiteCreditoByNumeroConta("123456");
    }

    @Test
    @DisplayName("Should return true when credit value equals limit")
    void deveRetornarTrueQuandoCreditoIgualAoLimite() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.CREDITO);
        transacao.setValor(100L);
        when(contaRepository.getLimiteCreditoByNumeroConta("123456")).thenReturn(100L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Should return false when credit value exceeds limit by 1")
    void deveRetornarFalseQuandoCreditoExcedeLimiteEm1() throws Exception {
        transacao.setTipoTransacao(TipoTransacao.CREDITO);
        when(contaRepository.getLimiteCreditoByNumeroConta("123456")).thenReturn(99L);

        boolean resultado = transacaoService.validarFundos(transacao);

        assertFalse(resultado);
    }

    // ========== validarFundos - INVALID TYPE ==========

    @Test
    @DisplayName("Should throw exception when transaction type is invalid")
    void deveLanguarExceptionQuandoTipoTransacaoInvalido() {
        transacao.setTipoTransacao(null);

        assertThrows(Exception.class, () -> transacaoService.validarFundos(transacao));
    }
    // ========== validarFraude ==========

    @Test
    @DisplayName("Should return true when no validators detect fraud")
    void deveRetornarTrueQuandoNenhumValidatorDetectaFraude() {
        when(fraudeValidator1.validate(transacao)).thenReturn(false);
        when(fraudeValidator2.validate(transacao)).thenReturn(false);

        List<FraudeValidators> validators = List.of(fraudeValidator1, fraudeValidator2);
        transacaoService = new TransacaoService(contaRepository, validators);

        boolean resultado = transacaoService.validarFraude(transacao);

        assertTrue(resultado);
        verify(fraudeValidator1, times(1)).validate(transacao);
        verify(fraudeValidator2, times(1)).validate(transacao);
    }

    @Test
    @DisplayName("Should return false when first validator detects fraud")
    void deveRetornarFalseQuandoPrimeiroValidatorDetectaFraude() {
        when(fraudeValidator1.validate(transacao)).thenReturn(true);

        List<FraudeValidators> validators = List.of(fraudeValidator1, fraudeValidator2);
        transacaoService = new TransacaoService(contaRepository, validators);

        boolean resultado = transacaoService.validarFraude(transacao);

        assertFalse(resultado);
        verify(fraudeValidator1, times(1)).validate(transacao);
    }

    @Test
    @DisplayName("Should return false when second validator detects fraud")
    void deveRetornarFalseQuandoSegundoValidatorDetectaFraude() {
        when(fraudeValidator1.validate(transacao)).thenReturn(false);
        when(fraudeValidator2.validate(transacao)).thenReturn(true);

        List<FraudeValidators> validators = List.of(fraudeValidator1, fraudeValidator2);
        transacaoService = new TransacaoService(contaRepository, validators);

        boolean resultado = transacaoService.validarFraude(transacao);

        assertFalse(resultado);
        verify(fraudeValidator1, times(1)).validate(transacao);
        verify(fraudeValidator2, times(1)).validate(transacao);
    }

    @Test
    @DisplayName("Should return true when validators list is empty")
    void deveRetornarTrueQuandoListaValidadoresVazia() {
        transacaoService = new TransacaoService(contaRepository, Collections.emptyList());

        boolean resultado = transacaoService.validarFraude(transacao);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Should throw NullPointerException when transaction type is null")
    void deveLanguarExceptionComMensagemCorretaParaTipoInvalido() {
        transacao.setTipoTransacao(null);

        assertThrows(NullPointerException.class, () -> transacaoService.validarFundos(transacao));
    }

    @Test
    @DisplayName("Should return false when first validator detects fraud")
    void deveRetornarFalseQuandoTodosValidatoresDetectamFraude() {
        when(fraudeValidator1.validate(transacao)).thenReturn(true);

        List<FraudeValidators> validators = List.of(fraudeValidator1, fraudeValidator2);
        transacaoService = new TransacaoService(contaRepository, validators);

        boolean resultado = transacaoService.validarFraude(transacao);

        assertFalse(resultado);
    }
}
