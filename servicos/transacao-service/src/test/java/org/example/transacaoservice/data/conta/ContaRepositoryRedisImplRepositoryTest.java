package org.example.transacaoservice.data.conta;

import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContaRepositoryRedisImpl Repository Methods Tests")
class ContaRepositoryRedisImplRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, String, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContaRepositoryRedisImpl contaRepository;

    private static final String NUMERO_CONTA = "123456";
    private static final String CONTA_KEY = "numeroConta:" + NUMERO_CONTA;
    private static final String AGUARDANDO_KEY = "confirmacao:" + NUMERO_CONTA;
    private static final String TRANSACAO_KEY = "transacao:" + NUMERO_CONTA;

    private Transacao transacaoDebito;
    private Transacao transacaoCredito;

    @BeforeEach
    void setUp() throws Exception {
        transacaoDebito = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(10000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Supermercado XYZ")
                .historico(new LinkedHashMap<>())
                .build();

        transacaoCredito = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(50000L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja ABC")
                .historico(new LinkedHashMap<>())
                .build();

        lenient().when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Nested
    @DisplayName("updateSaldo - Atualização de Saldo")
    class UpdateSaldoTests {

        @Test
        @DisplayName("Deve atualizar saldo com sucesso quando saldo é suficiente")
        void deveAtualizarSaldoQuandoSuficiente() {
            Long saldoFinal = 5000L;
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(saldoFinal);

            boolean resultado = contaRepository.updateSaldo(transacaoDebito);

            assertTrue(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "saldo", -10000L);
        }

        @Test
        @DisplayName("Deve retornar false quando saldo é insuficiente")
        void deveRetornarFalseQuandoSaldoInsuficiente() {
            Long saldoNegativo = -500L;
            Long saldoRestaurado = 10000L;
            when(hashOperations.increment(anyString(), anyString(), anyLong()))
                    .thenReturn(saldoNegativo)
                    .thenReturn(saldoRestaurado);

            boolean resultado = contaRepository.updateSaldo(transacaoDebito);

            assertFalse(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "saldo", -10000L);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "saldo", 10000L);
        }

        @Test
        @DisplayName("Deve retornar true quando saldo fica zero")
        void deveRetornarTrueQuandoSaldoZero() {
            Long saldoZero = 0L;
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(saldoZero);

            boolean resultado = contaRepository.updateSaldo(transacaoDebito);

            assertTrue(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "saldo", -10000L);
        }
    }

    @Nested
    @DisplayName("updateLimiteCredito - Atualização de Limite de Crédito")
    class UpdateLimiteCreditoTests {

        @Test
        @DisplayName("Deve atualizar limite de crédito com sucesso quando limite é suficiente")
        void deveAtualizarLimiteCreditoQuandoSuficiente() {
            Long limiteFinal = 20000L;
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(limiteFinal);

            boolean resultado = contaRepository.updateLimiteCredito(transacaoCredito);

            assertTrue(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "limiteCredito", -50000L);
        }

        @Test
        @DisplayName("Deve retornar false quando limite de crédito é insuficiente")
        void deveRetornarFalseQuandoLimiteInsuficiente() {
            Long limiteNegativo = -1000L;
            Long limiteRestaurado = 50000L;
            when(hashOperations.increment(anyString(), anyString(), anyLong()))
                    .thenReturn(limiteNegativo)
                    .thenReturn(limiteRestaurado);

            boolean resultado = contaRepository.updateLimiteCredito(transacaoCredito);

            assertFalse(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "limiteCredito", -50000L);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "limiteCredito", 50000L);
        }

        @Test
        @DisplayName("Deve retornar true quando limite fica zero")
        void deveRetornarTrueQuandoLimiteZero() {
            Long limiteZero = 0L;
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(limiteZero);

            boolean resultado = contaRepository.updateLimiteCredito(transacaoCredito);

            assertTrue(resultado);
            verify(hashOperations, times(1)).increment(CONTA_KEY, "limiteCredito", -50000L);
        }
    }

    @Nested
    @DisplayName("setTransacaoToAguardanddoConfirmacao - Aguardando Confirmação")
    class SetTransacaoToAguardanddoConfirmacaoTests {

        @Test
        @DisplayName("Deve salvar transação na chave de aguardando confirmação")
        void deveSalvarTransacaoAguardandoConfirmacao() throws Exception {
            contaRepository.setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            verify(valueOperations, times(1)).set(AGUARDANDO_KEY, transacaoDebito.toString());
        }

        @Test
        @DisplayName("Deve usar ObjectMapper para serializar transação")
        void deveUsarObjectMapperParaSerializar() throws Exception {
            String jsonTransacao = "{\"numeroConta\":\"123456\"}";
            when(objectMapper.writeValueAsString(transacaoDebito)).thenReturn(jsonTransacao);

            contaRepository.setTransacaoToAguardanddoConfirmacao(transacaoDebito);

            verify(valueOperations, times(1)).set(AGUARDANDO_KEY, jsonTransacao);
        }
    }

    @Nested
    @DisplayName("confirmarTransacao - Confirmação de Transação")
    class ConfirmarTransacaoTests {

        @Test
        @DisplayName("Deve confirmar transação e salvar na chave de transação com TTL")
        void deveConfirmarTransacao() throws Exception {
            String jsonTransacao = transacaoDebito.toString();
            lenient().when(objectMapper.writeValueAsString(transacaoDebito)).thenReturn(jsonTransacao);
            doNothing().when(valueOperations).set(eq(TRANSACAO_KEY), eq(jsonTransacao), anyLong(), any());
            when(redisTemplate.delete(AGUARDANDO_KEY)).thenReturn(true);

            contaRepository.confirmarTransacao(transacaoDebito);

            verify(valueOperations, times(1)).set(eq(TRANSACAO_KEY), eq(jsonTransacao), eq(5L), any());
            verify(redisTemplate, times(1)).delete(AGUARDANDO_KEY);
        }

        @Test
        @DisplayName("Deve serializar transação com ObjectMapper ao confirmar")
        void deveSerializarTransacaoAoConfirmar() throws Exception {
            String jsonTransacao = "{\"numeroConta\":\"123456\"}";
            when(objectMapper.writeValueAsString(transacaoDebito)).thenReturn(jsonTransacao);
            when(redisTemplate.delete(AGUARDANDO_KEY)).thenReturn(true);

            contaRepository.confirmarTransacao(transacaoDebito);

            verify(objectMapper, times(1)).writeValueAsString(transacaoDebito);
        }
    }

    @Nested
    @DisplayName("estornarTransacao - Estorno de Transação")
    class EstornarTransacaoTests {

        @Test
        @DisplayName("Deve estornar transação de débito incrementando saldo")
        void deveEstornarTransacaoDebito() {
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(20000L);
            when(redisTemplate.delete(AGUARDANDO_KEY)).thenReturn(true);

            contaRepository.estornarTransacao(transacaoDebito);

            verify(hashOperations, times(1)).increment(CONTA_KEY, "saldo", 10000L);
            verify(redisTemplate, times(1)).delete(AGUARDANDO_KEY);
        }

        @Test
        @DisplayName("Deve estornar transação de crédito incrementando limite")
        void deveEstornarTransacaoCredito() {
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(100000L);
            when(redisTemplate.delete(AGUARDANDO_KEY)).thenReturn(true);

            contaRepository.estornarTransacao(transacaoCredito);

            verify(hashOperations, times(1)).increment(CONTA_KEY, "limiteCredito", 50000L);
            verify(redisTemplate, times(1)).delete(AGUARDANDO_KEY);
        }

        @Test
        @DisplayName("Deve excluir chave de aguardando confirmação ao estornar")
        void deveExcluirChaveAguardandoAoEstornar() {
            when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(20000L);
            when(redisTemplate.delete(AGUARDANDO_KEY)).thenReturn(true);

            contaRepository.estornarTransacao(transacaoDebito);

            verify(redisTemplate, times(1)).delete(AGUARDANDO_KEY);
        }
    }
}
