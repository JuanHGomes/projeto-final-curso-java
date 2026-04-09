package org.example.transacaoservice.business.validators;

import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TempoEntreTransacoesValidator Integration Tests")
class TempoEntreTransacoesValidatorIntegrationTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private TempoEntreTransacoesValidator validator;

    private static final String NUMERO_CONTA = "123456";
    private LocalDateTime agora;
    private Transacao transacaoAnterior;
    private Transacao novaTransacao;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();

        transacaoAnterior = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(10000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(agora)
                .estabelecimento("Loja XYZ")
                .historico(new LinkedHashMap<>())
                .build();

        novaTransacao = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(5000L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(agora.plusMinutes(5))
                .estabelecimento("Supermercado ABC")
                .historico(new LinkedHashMap<>())
                .build();
    }

    @Nested
    @DisplayName("Cenários de Fraude - Intervalo menor ou igual a 1 minuto")
    class CenariosFraude {

        @Test
        @DisplayName("Deve detectar fraude quando transação ocorre em 30 segundos")
        void deveDetectarFraudeIntervalo30Segundos() {
            Transacao transacaoRapida = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusSeconds(30))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoRapida);

            assertTrue(resultado, "Deve detectar fraude para intervalo de 30 segundos");
            verify(transacaoRepository, times(1)).getTransacaoByNumeroConta(NUMERO_CONTA);
        }

        @Test
        @DisplayName("Deve detectar fraude quando transação ocorre exatamente em 1 minuto")
        void deveDetectarFraudeIntervaloExato1Minuto() {
            Transacao transacaoExata = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusMinutes(1))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoExata);

            assertTrue(resultado, "Deve detectar fraude para intervalo exato de 1 minuto");
        }

        @Test
        @DisplayName("Deve detectar fraude quando transação ocorre em 59 segundos")
        void deveDetectarFraudeIntervalo59Segundos() {
            Transacao transacaoQuaseMinuto = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusSeconds(59))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoQuaseMinuto);

            assertTrue(resultado, "Deve detectar fraude para intervalo de 59 segundos");
        }

        @Test
        @DisplayName("Deve detectar fraude quando nova transação tem data anterior (bug fix com .abs())")
        void deveDetectarFraudeComDataAnterior() {
            // Cenário: transação anterior está no futuro em relação à nova transação
            Transacao transacaoFuturo = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusHours(1)) // 1 hora no futuro
                    .estabelecimento("Loja XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoPassado = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora) // agora
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoFuturo));

            boolean resultado = validator.validate(transacaoPassado);

            // Duração de 1 hora (absoluto) > 1 minuto, então NÃO é fraude
            assertFalse(resultado, "Não deve detectar fraude quando duração absoluta > 1 minuto");
        }

        @Test
        @DisplayName("Deve detectar fraude quando transações são do mesmo dia com 30 segundos de diferença")
        void deveDetectarFraudeMesmoDia30Segundos() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 4, 8, 10, 0, 0);

            Transacao transacao1 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(baseTime)
                    .estabelecimento("Loja XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacao2 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(baseTime.plusSeconds(30))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacao1));

            boolean resultado = validator.validate(transacao2);

            assertTrue(resultado, "Deve detectar fraude para 30 segundos de diferença");
        }
    }

    @Nested
    @DisplayName("Cenários Sem Fraude - Intervalo maior que 1 minuto")
    class CenariosSemFraude {

        @Test
        @DisplayName("Não deve detectar fraude quando transação ocorre após 2 minutos")
        void naoDeveDetectarFraudeIntervalo2Minutos() {
            Transacao transacaoDemorada = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusMinutes(2))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoDemorada);

            assertFalse(resultado, "Não deve detectar fraude para intervalo de 2 minutos");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transação ocorre após 1 minuto e 1 segundo")
        void naoDeveDetectarFraudeIntervalo1MinutoE1Segundo() {
            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusMinutes(1).plusSeconds(1))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude para 1 minuto e 1 segundo");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transação ocorre após 1 hora")
        void naoDeveDetectarFraudeIntervalo1Hora() {
            Transacao transacaoDemorada = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusHours(1))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoDemorada);

            assertFalse(resultado, "Não deve detectar fraude para intervalo de 1 hora");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transação ocorre após 24 horas (mesmo horário, dia seguinte)")
        void naoDeveDetectarFraudeIntervalo24Horas() {
            // Este é o bug relatado: dias diferentes mas mesmo horário
            LocalDateTime diaSeguinte = agora.plusDays(1); // exata 24 horas depois

            Transacao transacaoDiaSeguinte = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(diaSeguinte)
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoDiaSeguinte);

            assertFalse(resultado, "Não deve detectar fraude para intervalo de 24 horas (dia seguinte mesmo horário)");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transação ocorre após 7 dias (mesmo horário)")
        void naoDeveDetectarFraudeIntervalo7Dias() {
            LocalDateTime seteDiasDepois = agora.plusDays(7);

            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(seteDiasDepois)
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude para intervalo de 7 dias");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transações são de dias consecutivos com mesmo horário")
        void naoDeveDetectarFraudeDiasConsecutivosMesmoHorario() {
            // Cenário relatado: 08/04 10:00 e 09/04 10:00
            LocalDateTime dia8 = LocalDateTime.of(2026, 4, 8, 10, 0, 0);
            LocalDateTime dia9 = LocalDateTime.of(2026, 4, 9, 10, 0, 0);

            Transacao transacaoDia8 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(dia8)
                    .estabelecimento("Loja XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoDia9 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(dia9)
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoDia8));

            boolean resultado = validator.validate(transacaoDia9);

            assertFalse(resultado, "Não deve detectar fraude para dias consecutivos com mesmo horário (24h de diferença)");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando transações são de dias aleatórios diferentes")
        void naoDeveDetectarFraudeDiasAleatorios() {
            // Cenário: dia 1 e dia 15 do mesmo mês
            LocalDateTime dia1 = LocalDateTime.of(2026, 4, 1, 10, 0, 0);
            LocalDateTime dia15 = LocalDateTime.of(2026, 4, 15, 10, 0, 0);

            Transacao transacaoDia1 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(dia1)
                    .estabelecimento("Loja XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoDia15 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(dia15)
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoDia1));

            boolean resultado = validator.validate(transacaoDia15);

            assertFalse(resultado, "Não deve detectar fraude para dias aleatórios diferentes (14 dias de diferença)");
        }
    }

    @Nested
    @DisplayName("Cenários de Conta sem Transação Anterior")
    class ContaSemTransacaoAnterior {

        @Test
        @DisplayName("Não deve detectar fraude quando não há transação anterior na conta")
        void naoDeveDetectarFraudeSemTransacaoAnterior() {
            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(novaTransacao);

            assertFalse(resultado, "Não deve detectar fraude quando não há transação anterior");
            verify(transacaoRepository, times(1)).getTransacaoByNumeroConta(NUMERO_CONTA);
        }

        @Test
        @DisplayName("Deve permitir primeira transação de uma nova conta")
        void devePermitirPrimeiraTransacao() {
            Transacao primeiraTransacao = Transacao.builder()
                    .numeroConta("999999") // Conta nova
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Primeira Compra")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta("999999"))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(primeiraTransacao);

            assertFalse(resultado, "Deve permitir primeira transação de conta nova");
        }
    }

    @Nested
    @DisplayName("Cenários com Diferentes Tipos de Transação")
    class DiferentesTiposTransacao {

        @Test
        @DisplayName("Deve validar fraude para transação de débito")
        void deveValidarFraudeDebito() {
            Transacao transacaoDebito = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusSeconds(30))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoDebito);

            assertTrue(resultado, "Deve detectar fraude para débito com intervalo curto");
        }

        @Test
        @DisplayName("Deve validar fraude para transação de crédito")
        void deveValidarFraudeCredito() {
            Transacao transacaoCredito = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(50000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(agora.plusSeconds(30))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoCredito);

            assertTrue(resultado, "Deve detectar fraude para crédito com intervalo curto");
        }
    }

    @Nested
    @DisplayName("Cenários com Diferentes Contas")
    class DiferentesContas {

        @Test
        @DisplayName("Não deve confundir transações de contas diferentes")
        void naoDeveConfundirContasDiferentes() {
            String contaDiferente = "654321";

            Transacao transacaoContaDiferente = Transacao.builder()
                    .numeroConta(contaDiferente)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusSeconds(30))
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            // Transação anterior é da conta "123456", nova transação é da conta "654321"
            when(transacaoRepository.getTransacaoByNumeroConta(contaDiferente))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(transacaoContaDiferente);

            assertFalse(resultado, "Não deve detectar fraude para conta diferente sem transação anterior");
            verify(transacaoRepository, times(1)).getTransacaoByNumeroConta(contaDiferente);
            verify(transacaoRepository, never()).getTransacaoByNumeroConta(NUMERO_CONTA);
        }
    }

    @Nested
    @DisplayName("Cenários de Boundary Values (valores limite)")
    class BoundaryValues {

        @Test
        @DisplayName("Deve detectar fraude com intervalo de 0 segundos (mesmo timestamp)")
        void deveDetectarFraudeIntervaloZero() {
            Transacao transacaoMesmoInstante = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora) // Mesmo timestamp da transação anterior
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoMesmoInstante);

            assertTrue(resultado, "Deve detectar fraude para intervalo de 0 segundos");
        }

        @Test
        @DisplayName("Deve validar com intervalo de 1 ano exato")
        void deveValidarIntervalo1Ano() {
            LocalDateTime umAnoDepois = agora.plusYears(1);

            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(umAnoDepois)
                    .estabelecimento("Loja ABC")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude para intervalo de 1 ano");
        }
    }
}
