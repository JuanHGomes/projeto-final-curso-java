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
@DisplayName("Testes Unitários - ValidarFraudeDelegate")
class ValidarFraudeDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution execution;

    private ValidarFraudeDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ValidarFraudeDelegate(transacaoService);
    }

    @Nested
    @DisplayName("Validação de Fraude")
    class ValidacaoFraudeTests {

        @Test
        @DisplayName("Deve validar sem fraude e setar variaveis corretamente")
        void deveValidarSemFraude() throws Exception {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", false);

            Transacao transacao = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.validarFraude(transacao)).thenReturn(transacao);

            delegate.execute(execution);

            verify(execution).setVariable(TRANSACAO_VARIABLE, transacao);
            verify(execution).setVariable(IS_FRAUDE_VARIABLE, false);
            verify(execution).setVariable(RESULTADO_TRANSACAO, true);
        }

        @Test
        @DisplayName("Deve detectar fraude e setar resultado como false")
        void deveDetectarFraude() throws Exception {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FRAUDE", true);

            Transacao transacao = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.validarFraude(transacao)).thenReturn(transacao);

            delegate.execute(execution);

            verify(execution).setVariable(IS_FRAUDE_VARIABLE, true);
            verify(execution).setVariable(RESULTADO_TRANSACAO, false);
        }
    }

    private Transacao criarTransacao(LinkedHashMap<String, Boolean> historico) {
        return Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now())
                .historico(historico)
                .build();
    }
}
