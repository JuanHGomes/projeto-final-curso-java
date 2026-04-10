package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - EstornarTransacaoDelegate")
class EstornarTransacaoDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution execution;

    private EstornarTransacaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new EstornarTransacaoDelegate(transacaoService);
    }

    @Test
    @DisplayName("Deve estornar transacao com sucesso")
    void deveEstornarTransacaoComSucesso() throws Exception {
        Transacao transacao = Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .historico(new LinkedHashMap<>())
                .build();

        when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
        doNothing().when(transacaoService).estornarTransacao(transacao);

        delegate.execute(execution);

        verify(transacaoService, times(1)).estornarTransacao(transacao);
        // EstornarTransacaoDelegate não seta variáveis na execution
        verify(execution, never()).setVariable(anyString(), any());
    }
}
