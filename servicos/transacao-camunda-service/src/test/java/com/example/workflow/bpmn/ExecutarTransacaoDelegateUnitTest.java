package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static com.example.workflow.commons.Camundakeys.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - ExecutarTransacaoDelegate")
class ExecutarTransacaoDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution execution;

    private ExecutarTransacaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ExecutarTransacaoDelegate(transacaoService);
    }

    @Nested
    @DisplayName("Execução de Transação")
    class ExecucaoTests {

        @Test
        @DisplayName("Deve executar transacao com sucesso e setar variaveis")
        void deveExecutarTransacaoComSucesso() throws Exception {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", true);

            Transacao transacao = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.executarTransacao(transacao)).thenReturn(transacao);

            delegate.execute(execution);

            verify(execution).setVariable(TRANSACAO_VARIABLE, transacao);
            verify(execution).setVariable(IS_EXECUCAO_SUCESSO_VARIABLE, true);
            verify(execution).setVariable(RESULTADO_TRANSACAO, true);
        }

        @Test
        @DisplayName("Deve setar variaveis como false quando execucao falhar")
        void deveSetarFalseQuandoExecucaoFalhar() throws Exception {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("EXECUCAO_SUCESSO", false);

            Transacao transacao = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.executarTransacao(transacao)).thenReturn(transacao);

            delegate.execute(execution);

            verify(execution).setVariable(IS_EXECUCAO_SUCESSO_VARIABLE, false);
            verify(execution).setVariable(RESULTADO_TRANSACAO, false);
        }
    }

    private Transacao criarTransacao(LinkedHashMap<String, Boolean> historico) {
        return Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .historico(historico)
                .build();
    }
}
