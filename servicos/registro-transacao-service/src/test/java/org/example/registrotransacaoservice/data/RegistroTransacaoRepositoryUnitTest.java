package org.example.registrotransacaoservice.data;

import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroTransacaoRepositoryUnitTest {

    @Mock
    private RegistroTransacaoDao dao;

    @Mock
    private RegistroTransacaoMapper mapper;

    @InjectMocks
    private RegistroTransacaoRepository repository;

    @Test
    @DisplayName("Deve registrar transação quando não existir")
    void deveRegistrarTransacaoQuandoNaoExistir() {
        // given
        Transacao transacao = Transacao.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .historico(new LinkedHashMap<>(Map.of("processado", true)))
                .build();

        TransacaoDocument document = TransacaoDocument.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .historico(new LinkedHashMap<>(Map.of("processado", true)))
                .build();

        when(dao.findTransacaoDocumentByNumeroContaAndTimeStamp("12345", LocalDateTime.of(2024, 1, 15, 10, 30)))
                .thenReturn(Optional.empty());
        when(mapper.toDocument(transacao)).thenReturn(document);
        when(dao.save(document)).thenReturn(document);
        when(mapper.toTransacao(document)).thenReturn(transacao);

        // when
        Transacao resultado = repository.registrarSeNaoPresente(transacao);

        // then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroConta()).isEqualTo("12345");
        verify(dao).save(document);
    }

    @Test
    @DisplayName("Não deve salvar quando transação já existe")
    void naoDeveSalvarQuandoTransacaoJaExiste() {
        // given
        Transacao transacao = Transacao.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .build();

        TransacaoDocument document = TransacaoDocument.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .build();

        Transacao transacaoExistente = Transacao.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .build();

        when(dao.findTransacaoDocumentByNumeroContaAndTimeStamp("12345", LocalDateTime.of(2024, 1, 15, 10, 30)))
                .thenReturn(Optional.of(document));
        when(mapper.toTransacao(document)).thenReturn(transacaoExistente);

        // when
        Transacao resultado = repository.registrarSeNaoPresente(transacao);

        // then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroConta()).isEqualTo("12345");
        verify(dao, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar transações dos últimos 30 dias")
    void deveBuscarTransacoesUltimosTrintaDias() {
        // given
        String numeroConta = "12345";
        LocalDateTime agora = LocalDateTime.now();

        TransacaoDocument doc1 = TransacaoDocument.builder()
                .numeroConta(numeroConta)
                .valor(5000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(agora.minusDays(5))
                .estabelecimento("Farmácia")
                .build();

        TransacaoDocument doc2 = TransacaoDocument.builder()
                .numeroConta(numeroConta)
                .valor(15000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(agora.minusDays(10))
                .estabelecimento("Posto")
                .build();

        Transacao transacao1 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(5000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(agora.minusDays(5))
                .estabelecimento("Farmácia")
                .build();

        Transacao transacao2 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(15000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(agora.minusDays(10))
                .estabelecimento("Posto")
                .build();

        LocalDateTime hoje = LocalDate.now().atTime(23, 59);
        LocalDateTime trintaDiasAtras = hoje.minusDays(30);

        when(dao.findByNumeroContaAndTimeStampBetween(eq(numeroConta), eq(trintaDiasAtras), eq(hoje)))
                .thenReturn(List.of(doc1, doc2));
        when(mapper.toTransacao(doc1)).thenReturn(transacao1);
        when(mapper.toTransacao(doc2)).thenReturn(transacao2);

        // when
        List<Transacao> resultado = repository.findAllOverLastThirtyDaysByNumeroConta(numeroConta);

        // then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Transacao::getEstabelecimento)
                .containsExactly("Farmácia", "Posto");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há transações no período")
    void deveRetornarListaVaziaQuandoNaoHaTransacoesNoPeriodo() {
        // given
        String numeroConta = "99999";
        LocalDateTime hoje = LocalDate.now().atTime(23, 59);
        LocalDateTime trintaDiasAtras = hoje.minusDays(30);

        when(dao.findByNumeroContaAndTimeStampBetween(numeroConta, trintaDiasAtras, hoje))
                .thenReturn(List.of());

        // when
        List<Transacao> resultado = repository.findAllOverLastThirtyDaysByNumeroConta(numeroConta);

        // then
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve mapear corretamente de Document para Transacao")
    void deveMapearDocumentParaTransacao() {
        // given
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("validado", true);
        historico.put("processado", true);

        TransacaoDocument document = TransacaoDocument.builder()
                .id("doc-id-123")
                .numeroConta("11111")
                .valor(50000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 3, 10, 9, 15))
                .estabelecimento("Loja A")
                .historico(historico)
                .build();

        Transacao transacaoEsperada = Transacao.builder()
                .numeroConta("11111")
                .valor(50000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 3, 10, 9, 15))
                .estabelecimento("Loja A")
                .historico(historico)
                .build();

        when(mapper.toTransacao(document)).thenReturn(transacaoEsperada);

        // when
        Transacao resultado = mapper.toTransacao(document);

        // then
        assertThat(resultado.getNumeroConta()).isEqualTo("11111");
        assertThat(resultado.getValor()).isEqualTo(50000L);
        assertThat(resultado.getTipoTransacao()).isEqualTo(Transacao.TipoTransacao.DEBITO);
        assertThat(resultado.getEstabelecimento()).isEqualTo("Loja A");
        assertThat(resultado.getHistorico()).containsEntry("validado", true);
    }

    @Test
    @DisplayName("Deve mapear corretamente de Transacao para Document")
    void deveMapearTransacaoParaDocument() {
        // given
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("validado", true);

        Transacao transacao = Transacao.builder()
                .numeroConta("22222")
                .valor(30000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.of(2024, 4, 20, 14, 30))
                .estabelecimento("Restaurante")
                .historico(historico)
                .build();

        TransacaoDocument documentEsperado = TransacaoDocument.builder()
                .numeroConta("22222")
                .valor(30000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.of(2024, 4, 20, 14, 30))
                .estabelecimento("Restaurante")
                .historico(historico)
                .build();

        when(mapper.toDocument(transacao)).thenReturn(documentEsperado);

        // when
        TransacaoDocument resultado = mapper.toDocument(transacao);

        // then
        assertThat(resultado.getNumeroConta()).isEqualTo("22222");
        assertThat(resultado.getValor()).isEqualTo(30000L);
        assertThat(resultado.getTipoTransacao()).isEqualTo(Transacao.TipoTransacao.CREDITO);
        assertThat(resultado.getEstabelecimento()).isEqualTo("Restaurante");
    }
}
