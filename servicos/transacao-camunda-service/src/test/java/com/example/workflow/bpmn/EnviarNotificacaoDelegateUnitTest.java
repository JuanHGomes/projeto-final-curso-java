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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - EnviarNotificacaoDelegate")
class EnviarNotificacaoDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution execution;

    private EnviarNotificacaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new EnviarNotificacaoDelegate(transacaoService);
    }

    @Test
    @DisplayName("Deve enviar notificacao com sucesso")
    void deveEnviarNotificacaoComSucesso() throws Exception {
        LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
        historico.put("EXECUCAO_SUCESSO", true);

        Transacao transacao = Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja Teste")
                .historico(historico)
                .build();

        when(execution.getVariable("TRANSACAO")).thenReturn(transacao);
        doNothing().when(transacaoService).enviarNotificacao(transacao);

        delegate.execute(execution);

        verify(transacaoService, times(1)).enviarNotificacao(transacao);
    }
}
