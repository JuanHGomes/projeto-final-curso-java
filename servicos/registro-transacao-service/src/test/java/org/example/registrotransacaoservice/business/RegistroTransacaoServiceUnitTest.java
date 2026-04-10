package org.example.registrotransacaoservice.business;

import org.example.registrotransacaoservice.client.TransacaoServiceClient;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.RegistroTransacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroTransacaoServiceUnitTest {

    @Mock
    private RegistroTransacaoRepository registroTransacaoRepository;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private TransacaoServiceClient transacaoServiceClient;

    @InjectMocks
    private RegistroTransacaoService registroTransacaoService;

    @Test
    @DisplayName("Deve garantir registro de transação chamando o repositório")
    void deveGarantirRegistroDeTransacao() {
        // given
        Transacao transacao = Transacao.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Supermercado")
                .historico(new LinkedHashMap<>(Map.of("processado", true)))
                .build();

        when(registroTransacaoRepository.registrarSeNaoPresente(transacao)).thenReturn(transacao);

        // when
        registroTransacaoService.garantirRegistroTransacao(transacao);

        // then
        verify(registroTransacaoRepository).registrarSeNaoPresente(transacao);
    }

    @Test
    @DisplayName("Deve retornar extrato dos últimos 30 dias")
    void deveRetornarExtratoUltimosTrintaDias() {
        // given
        String numeroConta = "12345";
        Transacao t1 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now().minusDays(5))
                .estabelecimento("Farmácia")
                .build();

        Transacao t2 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(20000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now().minusDays(10))
                .estabelecimento("Posto")
                .build();

        when(registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta))
                .thenReturn(List.of(t1, t2));

        // when
        List<Transacao> resultado = registroTransacaoService.getExtratoUltimosTrintaDias(numeroConta);

        // then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Transacao::getEstabelecimento)
                .containsExactly("Farmácia", "Posto");
        verify(registroTransacaoRepository).findAllOverLastThirtyDaysByNumeroConta(numeroConta);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há transações")
    void deveRetornarListaVaziaQuandoNaoHaTransacoes() {
        // given
        String numeroConta = "99999";
        when(registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta))
                .thenReturn(List.of());

        // when
        List<Transacao> resultado = registroTransacaoService.getExtratoUltimosTrintaDias(numeroConta);

        // then
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar saldo do transacao-service")
    void deveRetornarSaldo() {
        // given
        String numeroConta = "12345";
        when(transacaoServiceClient.getSaldo(numeroConta)).thenReturn(150000L);

        // when
        Long resultado = registroTransacaoService.getSaldo(numeroConta);

        // then
        assertThat(resultado).isEqualTo(150000L);
        verify(transacaoServiceClient).getSaldo(numeroConta);
    }

    @Test
    @DisplayName("Deve retornar limite de crédito do transacao-service")
    void deveRetornarLimiteCredito() {
        // given
        String numeroConta = "12345";
        when(transacaoServiceClient.getLimiteCredito(numeroConta)).thenReturn(500000L);

        // when
        Long resultado = registroTransacaoService.getLimiteCredito(numeroConta);

        // then
        assertThat(resultado).isEqualTo(500000L);
        verify(transacaoServiceClient).getLimiteCredito(numeroConta);
    }

    @Test
    @DisplayName("Deve gerar PDF do extrato")
    void deveGerarPdfDoExtrato() {
        // given
        String numeroConta = "12345";
        Transacao t1 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now().minusDays(5))
                .estabelecimento("Supermercado")
                .build();

        when(registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta))
                .thenReturn(List.of(t1));
        when(templateEngine.process(eq("extrato-bancario.html"), any(Context.class)))
                .thenReturn("<html><body>Extrato</body></html>");

        // when
        byte[] resultado = registroTransacaoService.getExtratoPdf(numeroConta);

        // then
        assertThat(resultado).isNotNull();
        assertThat(resultado).isNotEmpty();
        // Verifica que começa com o header de PDF (%PDF)
        assertThat(new String(resultado, 0, Math.min(4, resultado.length))).startsWith("%PDF");
    }

    @Test
    @DisplayName("Deve gerar PDF da fatura apenas com transações de crédito")
    void deveGerarPdfDaFaturaApenasComCredito() {
        // given
        String numeroConta = "12345";
        Transacao t1 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(50000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now().minusDays(5))
                .estabelecimento("Loja de Roupas")
                .build();

        Transacao t2 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(15000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now().minusDays(3))
                .estabelecimento("Supermercado")
                .build();

        when(registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta))
                .thenReturn(List.of(t1, t2));
        when(templateEngine.process(eq("extrato-cartao.html"), any(Context.class)))
                .thenReturn("<html><body>Fatura</body></html>");

        // when
        byte[] resultado = registroTransacaoService.getFaturaPdf(numeroConta);

        // then
        assertThat(resultado).isNotNull();
        assertThat(resultado).isNotEmpty();
        assertThat(new String(resultado, 0, Math.min(4, resultado.length))).startsWith("%PDF");
    }
}
