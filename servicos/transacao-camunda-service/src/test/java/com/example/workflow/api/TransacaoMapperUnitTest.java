package com.example.workflow.api;

import com.example.workflow.api.model.TransacaoRequest;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.business.model.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários - TransacaoMapper")
class TransacaoMapperUnitTest {

    private final TransacaoMapper mapper = new TransacaoMapper();

    @Test
    @DisplayName("Deve mapear TransacaoRequest para Transacao corretamente")
    void deveMapearRequestParaTransacao() {
        // given
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("FUNDOS_SUFICIENTES", true);
        historico.put("FRAUDE", false);
        historico.put("EXECUCAO_SUCESSO", true);

        LocalDateTime timeStamp = LocalDateTime.of(2024, 1, 15, 10, 30);

        TransacaoRequest request = new TransacaoRequest(
                "123456",
                5000L,
                TipoTransacao.DEBITO,
                timeStamp,
                "Supermercado",
                historico
        );

        // when
        Transacao resultado = mapper.toTransacao(request);

        // then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumeroConta()).isEqualTo("123456");
        assertThat(resultado.getValor()).isEqualTo(5000L);
        assertThat(resultado.getTipoTransacao()).isEqualTo(TipoTransacao.DEBITO);
        assertThat(resultado.getTimeStamp()).isEqualTo(timeStamp);
        assertThat(resultado.getEstabelecimento()).isEqualTo("Supermercado");
        assertThat(resultado.getHistorico()).containsEntry("FUNDOS_SUFICIENTES", true);
        assertThat(resultado.getHistorico()).containsEntry("FRAUDE", false);
        assertThat(resultado.getHistorico()).containsEntry("EXECUCAO_SUCESSO", true);
    }

    @Test
    @DisplayName("Deve mapear request com tipo CREDITO")
    void deveMapearRequestComTipoCredito() {
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("FUNDOS_SUFICIENTES", true);

        TransacaoRequest request = new TransacaoRequest(
                "789012",
                10000L,
                TipoTransacao.CREDITO,
                LocalDateTime.now(),
                "Loja de Roupas",
                historico
        );

        Transacao resultado = mapper.toTransacao(request);

        assertThat(resultado.getTipoTransacao()).isEqualTo(TipoTransacao.CREDITO);
        assertThat(resultado.getValor()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("Deve mapear request com estabelecimento nulo")
    void deveMapearRequestComEstabelecimentoNulo() {
        TransacaoRequest request = new TransacaoRequest(
                "111222",
                100L,
                TipoTransacao.DEBITO,
                LocalDateTime.now(),
                null,
                new LinkedHashMap<>()
        );

        Transacao resultado = mapper.toTransacao(request);

        assertThat(resultado.getNumeroConta()).isEqualTo("111222");
        assertThat(resultado.getEstabelecimento()).isNull();
    }
}
