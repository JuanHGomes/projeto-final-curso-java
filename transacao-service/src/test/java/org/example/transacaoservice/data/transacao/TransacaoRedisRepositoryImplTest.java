package org.example.transacaoservice.data.transacao;

import org.example.transacaoservice.business.model.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoRedisRepositoryImpl Tests")
class TransacaoRedisRepositoryImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransacaoRedisRepositoryImpl transacaoRepository;

    private static final String NUMERO_CONTA = "123456";
    private static final String TRANSACAO_PREFIX = "transacao:";
    private static final String REDIS_KEY = TRANSACAO_PREFIX + NUMERO_CONTA;

    private Transacao criarTransacao() {
        Transacao transacao = new Transacao();
        transacao.setNumeroConta(NUMERO_CONTA);
        transacao.setValor(100L);
        transacao.setTimeStamp(LocalDateTime.now());
        return transacao;
    }

    // ========== save ==========

    @Test
    @DisplayName("Should return null when saving transaction")
    void deveRetornarNullAoSalvarTransacao() {
        Transacao transacao = criarTransacao();

        Transacao resultado = transacaoRepository.save(transacao);

        assertNull(resultado);
    }

    // ========== getTimeStampByNumeroConta ==========

    @Test
    @DisplayName("Should return timestamp when transaction exists")
    void deveRetornarTimeStampQuandoTransacaoExiste() throws Exception {
        Transacao transacao = criarTransacao();
        LocalDateTime timeStamp = transacao.getTimeStamp();
        String json = "{\"numeroConta\":\"123456\",\"valor\":100}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isPresent());
        assertEquals(timeStamp, resultado.get());
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(REDIS_KEY);
        verify(objectMapper, times(1)).readValue(json, Transacao.class);
    }

    @Test
    @DisplayName("Should return empty optional when key does not exist")
    void deveRetornarEmptyOptionalQuandoChaveNaoExiste() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isEmpty());
        verify(valueOperations, times(1)).get(REDIS_KEY);
        verify(objectMapper, never()).readValue(anyString(), eq(Transacao.class));
    }

    @Test
    @DisplayName("Should return empty optional when timestamp is null")
    void deveRetornarEmptyOptionalQuandoTimeStampNulo() throws Exception {
        Transacao transacao = new Transacao();
        transacao.setNumeroConta(NUMERO_CONTA);
        transacao.setTimeStamp(null);
        String json = "{\"numeroConta\":\"123456\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Should call redis with correct key for getTimeStampByNumeroConta")
    void deveCallRedisComChaveCorretaParaGetTimeStamp() throws Exception {
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"123456\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

        verify(valueOperations, times(1)).get("transacao:123456");
    }

    @Test
    @DisplayName("Should retrieve timestamp for different account numbers")
    void deveRetornarTimeStampParaDiferentesNumerosContas() throws Exception {
        String numeroConta2 = "654321";
        String redisKey2 = TRANSACAO_PREFIX + numeroConta2;
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"654321\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey2)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(numeroConta2);

        assertTrue(resultado.isPresent());
        verify(valueOperations, times(1)).get(redisKey2);
    }

    // ========== getTransacaoByNumeroConta ==========

    @Test
    @DisplayName("Should return transaction when exists in redis")
    void deveRetornarTransacaoQuandoExisteRedis() throws Exception {
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"123456\",\"valor\":100}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isPresent());
        assertEquals(transacao, resultado.get());
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(REDIS_KEY);
        verify(objectMapper, times(1)).readValue(json, Transacao.class);
    }

    @Test
    @DisplayName("Should return empty optional when transaction not found")
    void deveRetornarEmptyOptionalQuandoTransacaoNaoEncontrada() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isEmpty());
        verify(valueOperations, times(1)).get(REDIS_KEY);
        verify(objectMapper, never()).readValue(anyString(), eq(Transacao.class));
    }

    @Test
    @DisplayName("Should return transaction with all fields populated")
    void deveRetornarTransacaoComTodosCamposPreenchidos() throws Exception {
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"123456\",\"valor\":100,\"timestamp\":\"2024-01-01T10:00:00\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

        assertTrue(resultado.isPresent());
        assertEquals(NUMERO_CONTA, resultado.get().getNumeroConta());
        assertEquals(100L, resultado.get().getValor());
        assertNotNull(resultado.get().getTimeStamp());
    }

    @Test
    @DisplayName("Should call redis with correct key for getTransacaoByNumeroConta")
    void deveCallRedisComChaveCorretaParaGetTransacao() throws Exception {
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"123456\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

        verify(valueOperations, times(1)).get("transacao:123456");
    }

    @Test
    @DisplayName("Should retrieve transaction for different account numbers")
    void deveRetornarTransacaoParaDiferentesNumerosContas() throws Exception {
        String numeroConta2 = "654321";
        String redisKey2 = TRANSACAO_PREFIX + numeroConta2;
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"654321\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey2)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(numeroConta2);

        assertTrue(resultado.isPresent());
        verify(valueOperations, times(1)).get(redisKey2);
    }

    @Test
    @DisplayName("Should use correct ObjectMapper class type")
    void deveUsarTipoCorretoObjectMapper() throws Exception {
        Transacao transacao = criarTransacao();
        String json = "{\"numeroConta\":\"123456\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class)).thenReturn(transacao);

        transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

        verify(objectMapper, times(1)).readValue(json, Transacao.class);
    }

    @Test
    @DisplayName("Should handle exception during JSON deserialization")
    void deveLanguarExceptionAoDeserializarJson() throws Exception {
        String json = "{\"invalid json}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(json);
        when(objectMapper.readValue(json, Transacao.class))
                .thenThrow(new RuntimeException("JSON parse error"));

        assertThrows(RuntimeException.class,
                () -> transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA));
    }

}