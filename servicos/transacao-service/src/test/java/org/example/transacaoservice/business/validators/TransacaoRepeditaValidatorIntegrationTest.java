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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoRepeditaValidator Integration Tests")
class TransacaoRepeditaValidatorIntegrationTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private TransacaoRepeditaValidator validator;

    private static final String NUMERO_CONTA = "123456";
    private LocalDateTime agora;
    private Transacao transacaoAnterior;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();

        transacaoAnterior = Transacao.builder()
                .numeroConta(NUMERO_CONTA)
                .valor(10000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(agora.minusMinutes(5))
                .estabelecimento("Supermercado XYZ")
                .historico(new LinkedHashMap<>())
                .build();
    }

    @Nested
    @DisplayName("Cenários de Fraude - Transação Repetida")
    class CenariosFraude {

        @Test
        @DisplayName("Deve detectar fraude quando valor e estabelecimento são idênticos")
        void deveDetectarFraudeValorEEstabelecimentoIdentico() {
            Transacao transacaoRepetida = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L) // Mesmo valor
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ") // Mesmo estabelecimento
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoRepetida);

            assertTrue(resultado, "Deve detectar fraude para valor e estabelecimento idênticos");
            verify(transacaoRepository, times(1)).getTransacaoByNumeroConta(NUMERO_CONTA);
        }

        @Test
        @DisplayName("Deve detectar fraude mesmo com tipo de transação diferente")
        void deveDetectarFraudeMesmoValorEEstabelecimentoTipoDiferente() {
            Transacao transacaoRepetida = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L) // Mesmo valor
                    .tipoTransacao(TipoTransacao.CREDITO) // Tipo diferente
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ") // Mesmo estabelecimento
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoRepetida);

            assertTrue(resultado, "Deve detectar fraude mesmo com tipo de transação diferente");
        }

        @Test
        @DisplayName("Deve detectar fraude mesmo com timestamp diferente")
        void deveDetectarFraudeMesmoValorEEstabelecimentoTimestampDiferente() {
            Transacao transacaoRepetida = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusHours(2)) // Timestamp bem diferente
                    .estabelecimento("Supermercado XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoRepetida);

            assertTrue(resultado, "Deve detectar fraude mesmo com timestamp diferente");
        }

        @Test
        @DisplayName("Deve detectar fraude para transações consecutivas idênticas")
        void deveDetectarFraudeTransacoesConsecutivasIdentica() {
            // Simula envio duplicado da mesma transação
            Transacao transacaoDuplicada = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoDuplicada);

            assertTrue(resultado, "Deve detectar transação duplicada");
        }
    }

    @Nested
    @DisplayName("Cenários Sem Fraude - Valor ou Estabelecimento Diferentes")
    class CenariosSemFraude {

        @Test
        @DisplayName("Não deve detectar fraude quando valor é diferente")
        void naoDeveDetectarFraudeValorDiferente() {
            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(15000L) // Valor diferente
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude quando valor é diferente");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando estabelecimento é diferente")
        void naoDeveDetectarFraudeEstabelecimentoDiferente() {
            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Farmácia ABC") // Estabelecimento diferente
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude quando estabelecimento é diferente");
        }

        @Test
        @DisplayName("Não deve detectar fraude quando ambos valor e estabelecimento são diferentes")
        void naoDeveDetectarFraudeAmbosDiferentes() {
            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(25000L) // Valor diferente
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(agora)
                    .estabelecimento("Restaurante DEF") // Estabelecimento diferente
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude quando ambos são diferentes");
        }

        @Test
        @DisplayName("Não deve detectar fraude para compras em lojas diferentes no mesmo dia")
        void naoDeveDetectarFraudeLojasDiferentesMesmoDia() {
            Transacao transacaoLoja1 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.minusHours(1))
                    .estabelecimento("Starbucks")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoLoja2 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(5000L) // Mesmo valor
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("McDonald's") // Estabelecimento diferente
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoLoja1));

            boolean resultado = validator.validate(transacaoLoja2);

            assertFalse(resultado, "Não deve detectar fraude para lojas diferentes no mesmo dia");
        }

        @Test
        @DisplayName("Não deve detectar fraude para mesmo estabelecimento com valores diferentes")
        void naoDeveDetectarFraudeMesmoEstabelecimentoValoresDiferentes() {
            Transacao transacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(8000L) // Valor diferente
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ") // Mesmo estabelecimento
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Não deve detectar fraude para mesmo estabelecimento com valores diferentes");
        }
    }

    @Nested
    @DisplayName("Cenários de Conta sem Transação Anterior")
    class ContaSemTransacaoAnterior {

        @Test
        @DisplayName("Não deve detectar fraude quando não há transação anterior")
        void naoDeveDetectarFraudeSemTransacaoAnterior() {
            Transacao primeiraTransacao = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Primeira Loja")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(primeiraTransacao);

            assertFalse(resultado, "Não deve detectar fraude para primeira transação");
            verify(transacaoRepository, times(1)).getTransacaoByNumeroConta(NUMERO_CONTA);
        }

        @Test
        @DisplayName("Deve permitir primeira transação de conta nova")
        void devePermitirPrimeiraTransacaoContaNova() {
            Transacao transacao = Transacao.builder()
                    .numeroConta("999999")
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Loja XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta("999999"))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(transacao);

            assertFalse(resultado, "Deve permitir primeira transação de conta nova");
        }
    }

    @Nested
    @DisplayName("Cenários com Valores Extremos")
    class ValoresExtremos {

        @Test
        @DisplayName("Deve detectar fraude para valor zero idêntico")
        void deveDetectarFraudeValorZero() {
            Transacao transacaoAnteriorZero = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(0L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.minusMinutes(5))
                    .estabelecimento("Loja Teste")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoZero = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(0L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Loja Teste")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnteriorZero));

            boolean resultado = validator.validate(transacaoZero);

            assertTrue(resultado, "Deve detectar fraude para valor zero idêntico");
        }

        @Test
        @DisplayName("Deve detectar fraude para valores muito altos idênticos")
        void deveDetectarFraudeValorAlto() {
            Long valorAlto = 999999999L;

            Transacao transacaoAnteriorAlta = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(valorAlto)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.minusMinutes(5))
                    .estabelecimento("Loja Luxo")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoAlta = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(valorAlto)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Loja Luxo")
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnteriorAlta));

            boolean resultado = validator.validate(transacaoAlta);

            assertTrue(resultado, "Deve detectar fraude para valores muito altos idênticos");
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
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ")
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

        @Test
        @DisplayName("Deve permitir transações idênticas em contas diferentes")
        void devePermitirTransacoesIdenticaContasDiferentes() {
            String conta1 = "111111";
            String conta2 = "222222";

            Transacao transacaoConta1 = Transacao.builder()
                    .numeroConta(conta1)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.minusMinutes(5))
                    .estabelecimento("Amazon")
                    .historico(new LinkedHashMap<>())
                    .build();

            Transacao transacaoConta2 = Transacao.builder()
                    .numeroConta(conta2)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Amazon")
                    .historico(new LinkedHashMap<>())
                    .build();

            // Cada conta tem sua própria transação anterior
            lenient().when(transacaoRepository.getTransacaoByNumeroConta(conta1))
                    .thenReturn(Optional.of(transacaoConta1));
            when(transacaoRepository.getTransacaoByNumeroConta(conta2))
                    .thenReturn(Optional.empty());

            boolean resultado = validator.validate(transacaoConta2);

            assertFalse(resultado, "Deve permitir transações idênticas em contas diferentes");
        }
    }

    @Nested
    @DisplayName("Cenários de Sensibilidade a Case (estabelecimento)")
    class SensibilidadeCase {

        @Test
        @DisplayName("Não deve detectar fraude quando estabelecimento tem case diferente")
        void naoDeveDetectarFraudeCaseDiferente() {
            // O validador usa String.equals(), que é case-sensitive
            Transacao transacaoCaseDiferente = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("supermercado xyz") // Case diferente
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoCaseDiferente);

            // Como usa equals(), "Supermercado XYZ" != "supermercado xyz"
            assertFalse(resultado, "Não deve detectar fraude para case diferente (validação é case-sensitive)");
        }

        @Test
        @DisplayName("Deve detectar fraude quando estabelecimento tem case idêntico")
        void deveDetectarFraudeCaseIdentico() {
            Transacao transacaoCaseIdentico = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ") // Exatamente igual
                    .historico(new LinkedHashMap<>())
                    .build();

            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado = validator.validate(transacaoCaseIdentico);

            assertTrue(resultado, "Deve detectar fraude para estabelecimento com case idêntico");
        }
    }

    @Nested
    @DisplayName("Cenários de Múltiplas Validações Consecutivas")
    class ValidacoesConsecutivas {

        @Test
        @DisplayName("Deve detectar fraude em múltiplas transações repetidas")
        void deveDetectarFraudeMultiplasTransacoes() {
            Transacao transacaoRepetida1 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora)
                    .estabelecimento("Supermercado XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            // Primeira validação
            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoAnterior));

            boolean resultado1 = validator.validate(transacaoRepetida1);
            assertTrue(resultado1, "Primeira transação repetida deve ser detectada");

            // Simula que a transação repetida foi armazenada
            when(transacaoRepository.getTransacaoByNumeroConta(NUMERO_CONTA))
                    .thenReturn(Optional.of(transacaoRepetida1));

            Transacao transacaoRepetida2 = Transacao.builder()
                    .numeroConta(NUMERO_CONTA)
                    .valor(10000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(agora.plusMinutes(1))
                    .estabelecimento("Supermercado XYZ")
                    .historico(new LinkedHashMap<>())
                    .build();

            // Segunda validação
            boolean resultado2 = validator.validate(transacaoRepetida2);
            assertTrue(resultado2, "Segunda transação repetida deve ser detectada");

            verify(transacaoRepository, times(2)).getTransacaoByNumeroConta(NUMERO_CONTA);
        }
    }
}
