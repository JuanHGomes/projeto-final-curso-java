package org.example.transacaoservice.data.conta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContaRepositoryRedisImpl Tests")
class ContaRepositoryRedisImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, String, Object> hashOperations;

    @InjectMocks
    private ContaRepositoryRedisImpl contaRepository;

    private static final String NUMERO_CONTA = "123456";
    private static final String CONTA_KEY = "numeroConta:" + NUMERO_CONTA;
    private static final Long SALDO = 1000L;
    private static final Long LIMITE_CREDITO = 5000L;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);
    }

    // ========== getSaldoByNumeroConta ==========

    @Test
    @DisplayName("Should return balance when account exists")
    void deveRetornarSaldoQuandoContaExiste() {
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(SALDO);

        Long resultado = contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        assertEquals(SALDO, resultado);
        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).get(CONTA_KEY, "saldo");
    }

    @Test
    @DisplayName("Should return zero when balance key does not exist")
    void deveRetornarNullQuandoChaveSaldoNaoExiste() {
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(null);

        Long resultado = contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        assertEquals(0L, resultado);
        verify(hashOperations, times(1)).get(CONTA_KEY, "saldo");
    }

    @Test
    @DisplayName("Should return zero balance")
    void deveRetornarSaldoZero() {
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(0L);

        Long resultado = contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        assertEquals(0L, resultado);
    }

    @Test
    @DisplayName("Should return negative balance")
    void deveRetornarSaldoNegativo() {
        Long saldoNegativo = -500L;
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(saldoNegativo);

        Long resultado = contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        assertEquals(saldoNegativo, resultado);
    }

    @Test
    @DisplayName("Should return large balance value")
    void deveRetornarSaldoGrande() {
        Long saldoGrande = 999999999L;
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(saldoGrande);

        Long resultado = contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        assertEquals(saldoGrande, resultado);
    }

    @Test
    @DisplayName("Should call redisTemplate opsForHash method")
    void deveCallRedisTemplateOpsForHash() {
        when(hashOperations.get(CONTA_KEY, "saldo")).thenReturn(SALDO);

        contaRepository.getSaldoByNumeroConta(NUMERO_CONTA);

        verify(redisTemplate, times(1)).opsForHash();
    }

    // ========== getLimiteCreditoByNumeroConta ==========

    @Test
    @DisplayName("Should return credit limit when account exists")
    void deveRetornarLimiteCreditoQuandoContaExiste() {
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(LIMITE_CREDITO);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        assertEquals(LIMITE_CREDITO, resultado);
        verify(redisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).get(CONTA_KEY, "limiteCredito");
    }

    @Test
    @DisplayName("Should return zero when credit limit key does not exist")
    void deveRetornarNullQuandoChaveLimiteCreditoNaoExiste() {
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(null);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        assertEquals(0L, resultado);
        verify(hashOperations, times(1)).get(CONTA_KEY, "limiteCredito");
    }

    @Test
    @DisplayName("Should return zero credit limit")
    void deveRetornarLimiteCreditoZero() {
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(0L);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        assertEquals(0L, resultado);
    }

    @Test
    @DisplayName("Should return negative credit limit")
    void deveRetornarLimiteCreditoNegativo() {
        Long limitoNegativo = -1000L;
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(limitoNegativo);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        assertEquals(limitoNegativo, resultado);
    }

    @Test
    @DisplayName("Should return large credit limit value")
    void deveRetornarLimiteCreditoGrande() {
        Long limiteGrande = 999999999L;
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(limiteGrande);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        assertEquals(limiteGrande, resultado);
    }

    @Test
    @DisplayName("Should call redisTemplate opsForHash method for credit limit")
    void deveCallRedisTemplateOpsForHashParaLimiteCredito() {
        when(hashOperations.get(CONTA_KEY, "limiteCredito")).thenReturn(LIMITE_CREDITO);

        contaRepository.getLimiteCreditoByNumeroConta(NUMERO_CONTA);

        verify(redisTemplate, times(1)).opsForHash();
    }

    // ========== Different Account Numbers ==========

    @Test
    @DisplayName("Should retrieve balance for different account numbers")
    void deveRetornarSaldoParaDiferentesNumerosContas() {
        String conta2 = "654321";
        String conta2Key = "numeroConta:" + conta2;
        Long saldo2 = 2000L;
        when(hashOperations.get(conta2Key, "saldo")).thenReturn(saldo2);

        Long resultado = contaRepository.getSaldoByNumeroConta(conta2);

        assertEquals(saldo2, resultado);
        verify(hashOperations, times(1)).get(conta2Key, "saldo");
    }

    @Test
    @DisplayName("Should retrieve credit limit for different account numbers")
    void deveRetornarLimiteCreditoParaDiferentesNumerosContas() {
        String conta2 = "654321";
        String conta2Key = "numeroConta:" + conta2;
        Long limite2 = 10000L;
        when(hashOperations.get(conta2Key, "limiteCredito")).thenReturn(limite2);

        Long resultado = contaRepository.getLimiteCreditoByNumeroConta(conta2);

        assertEquals(limite2, resultado);
        verify(hashOperations, times(1)).get(conta2Key, "limiteCredito");
    }

}