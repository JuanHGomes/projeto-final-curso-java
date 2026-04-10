package com.example.workflow.api;

import com.example.workflow.api.model.TransacaoRequest;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.business.model.Transacao;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static com.example.workflow.commons.Camundakeys.RESULTADO_TRANSACAO;
import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - CamundaController")
class CamundaControllerUnitTest {

    @Mock
    private TransacaoMapper mapper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ProcessInstance processInstance;

    @InjectMocks
    private CamundaController controller;

    @Test
    @DisplayName("Deve iniciar processo com transacao e retornar resultado true")
    void deveIniciarProcessoComSucesso() {
        // given
        TransacaoRequest request = new TransacaoRequest(
                "123456", 1000L, TipoTransacao.DEBITO,
                LocalDateTime.now(), "Loja Teste", new LinkedHashMap<>());

        Transacao transacao = Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(request.timeStamp())
                .estabelecimento("Loja Teste")
                .historico(request.historico())
                .build();

        when(mapper.toTransacao(request)).thenReturn(transacao);
        when(runtimeService.startProcessInstanceByKey(eq("transacao-process"), anyMap()))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("proc-id-123");
        when(runtimeService.getVariable("proc-id-123", RESULTADO_TRANSACAO)).thenReturn(true);

        // when
        var response = controller.Start(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
        verify(runtimeService).startProcessInstanceByKey(eq("transacao-process"), anyMap());
    }

    @Test
    @DisplayName("Deve retornar resultado false quando transacao falhar")
    void deveRetornarResultadoFalse() {
        TransacaoRequest request = new TransacaoRequest(
                "789012", 999999L, TipoTransacao.CREDITO,
                LocalDateTime.now(), "Loja Teste", new LinkedHashMap<>());

        Transacao transacao = Transacao.builder()
                .numeroConta("789012")
                .valor(999999L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(request.timeStamp())
                .historico(request.historico())
                .build();

        when(mapper.toTransacao(request)).thenReturn(transacao);
        when(runtimeService.startProcessInstanceByKey(eq("transacao-process"), anyMap()))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("proc-id-456");
        when(runtimeService.getVariable("proc-id-456", RESULTADO_TRANSACAO)).thenReturn(false);

        var response = controller.Start(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    @DisplayName("Deve mapear request para transacao corretamente")
    void deveMapearRequestCorretamente() {
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("FUNDOS_SUFICIENTES", true);

        TransacaoRequest request = new TransacaoRequest(
                "111222", 5000L, TipoTransacao.DEBITO,
                LocalDateTime.of(2024, 1, 15, 10, 30), "Supermercado", historico);

        Transacao transacaoEsperada = Transacao.builder()
                .numeroConta("111222")
                .valor(5000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .estabelecimento("Supermercado")
                .historico(historico)
                .build();

        when(mapper.toTransacao(request)).thenReturn(transacaoEsperada);
        when(runtimeService.startProcessInstanceByKey(eq("transacao-process"), anyMap()))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("proc-id-789");
        when(runtimeService.getVariable("proc-id-789", RESULTADO_TRANSACAO)).thenReturn(true);

        controller.Start(request);

        verify(mapper).toTransacao(request);
        verify(runtimeService).startProcessInstanceByKey(eq("transacao-process"), anyMap());
    }
}
