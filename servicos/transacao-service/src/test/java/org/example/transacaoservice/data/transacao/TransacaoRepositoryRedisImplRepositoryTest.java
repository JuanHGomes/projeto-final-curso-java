package org.example.transacaoservice.data.transacao;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoRepositoryRedisImpl Repository Tests")
class TransacaoRepositoryRedisImplRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransacaoRepositoryRedisImpl transacaoRepository;

    private static final String NUMERO_CONTA = "123456";
    private static final String TRANSACAO_KEY = "transacao:" + NUMERO_CONTA;

    private Transacao transacao;
    private String jsonTransacao;

    @BeforeEach
    void setUp() throws Exception {
        transacao = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(10000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2026, 4, 8, 10, 30, 0))
                .estabelecimento("Supermercado XYZ")
                .historico(new LinkedHashMap<>())
                .build();

        jsonTransacao = "{\"numeroConta\":\"123456\",\"valor\":10000}";

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("save - Salvamento de Transação")
    class SaveTests {

        @Test
        @DisplayName("Deve retornar null ao salvar transação (método não implementado)")
        void deveRetornarNullAoSalvar() {
            Transacao resultado = transacaoRepository.save(transacao);

            assertNull(resultado);
        }
    }

    @Nested
    @DisplayName("getTimeStampByNumeroConta - Busca de Timestamp")
    class GetTimeStampTests {

        @Test
        @DisplayName("Deve retornar timestamp quando transação existe")
        void deveRetornarTimestampQuandoTransacaoExiste() throws Exception {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(jsonTransacao);
            when(objectMapper.readValue(jsonTransacao, Transacao.class)).thenReturn(transacao);

            Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

            assertTrue(resultado.isPresent());
            assertEquals(transacao.getTimeStamp(), resultado.get());
            verify(valueOperations, times(1)).get(TRANSACAO_KEY);
        }

        @Test
        @DisplayName("Deve retornar Optional.empty quando transação não existe")
        void deveRetornarEmptyQuandoTransacaoNaoExiste() {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(null);

            Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Deve retornar Optional.empty quando timestamp é null")
        void deveRetornarEmptyQuandoTimestampNull() throws Exception {
            Transacao transacaoSemTimestamp = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(null)
                    .estabelecimento("Loja")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(jsonTransacao);
            when(objectMapper.readValue(jsonTransacao, Transacao.class)).thenReturn(transacaoSemTimestamp);

            Optional<LocalDateTime> resultado = transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Deve usar chave correta com prefixo")
        void deveUsarChaveComPrefixo() throws Exception {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(jsonTransacao);
            when(objectMapper.readValue(jsonTransacao, Transacao.class)).thenReturn(transacao);

            transacaoRepository.getTimeStampByNumeroConta(NUMERO_CONTA);

            verify(valueOperations, times(1)).get("transacao:123456");
        }
    }

    @Nested
    @DisplayName("getTransacaoByNumeroConta - Busca de Transação")
    class GetTransacaoTests {

        @Test
        @DisplayName("Deve retornar transação quando existe")
        void deveRetornarTransacaoQuandoExiste() throws Exception {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(jsonTransacao);
            when(objectMapper.readValue(jsonTransacao, Transacao.class)).thenReturn(transacao);

            Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

            assertTrue(resultado.isPresent());
            assertEquals(transacao.getNumeroConta(), resultado.get().getNumeroConta());
            assertEquals(transacao.getValor(), resultado.get().getValor());
            assertEquals(transacao.getTipoTransacao(), resultado.get().getTipoTransacao());
            assertEquals(transacao.getEstabelecimento(), resultado.get().getEstabelecimento());
        }

        @Test
        @DisplayName("Deve retornar Optional.empty quando transação não existe")
        void deveRetornarEmptyQuandoNaoExiste() {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(null);

            Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Deve usar ObjectMapper para deserializar JSON")
        void deveUsarObjectMapperParaDeserializar() throws Exception {
            when(valueOperations.get(TRANSACAO_KEY)).thenReturn(jsonTransacao);
            when(objectMapper.readValue(jsonTransacao, Transacao.class)).thenReturn(transacao);

            transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA);

            verify(objectMapper, times(1)).readValue(jsonTransacao, Transacao.class);
        }

        @Test
        @DisplayName("Deve buscar transação para conta diferente")
        void deveBuscarTransacaoContaDiferente() throws Exception {
            String contaDiferente = "654321";
            String transacaoKeyDiferente = "transacao:" + contaDiferente;
            String jsonDiferente = "{\"numeroConta\":\"654321\"}";

            Transacao transacaoDiferente = Transacao.builder()
                    .numeroConta(contaDiferente)
                    .valor(20000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(valueOperations.get(transacaoKeyDiferente)).thenReturn(jsonDiferente);
            when(objectMapper.readValue(jsonDiferente, Transacao.class)).thenReturn(transacaoDiferente);

            Optional<Transacao> resultado = transacaoRepository.getTransacaoByNumeroConta(contaDiferente);

            assertTrue(resultado.isPresent());
            assertEquals(contaDiferente, resultado.get().getNumeroConta());
            assertEquals(20000L, resultado.get().getValor());
            assertEquals(TipoTransacao.CREDITO, resultado.get().getTipoTransacao());
        }
    }
}
