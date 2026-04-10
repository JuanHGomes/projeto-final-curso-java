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

import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - EnviarRegistroTransacaoDelegate")
class EnviarRegistroTransacaoDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution delegateExecution;

    private EnviarRegistroTransacaoDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new EnviarRegistroTransacaoDelegate(transacaoService);
    }

    @Nested
    @DisplayName("Envio para Registro")
    class EnvioRegistroTests {

        @Test
        @DisplayName("Deve enviar transacao para registro com sucesso")
        void deveEnviarTransacaoParaRegistroComSucesso() throws Exception {
            Transacao transacao = criarTransacao();

            when(delegateExecution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.enviarTransacaoParaRegistro(transacao)).thenReturn(true);

            delegate.execute(delegateExecution);

            verify(transacaoService).enviarTransacaoParaRegistro(transacao);
            verify(delegateExecution).setVariable(TRANSACAO_VARIABLE, true);
        }

        @Test
        @DisplayName("Deve setar variavel como false quando registro falhar")
        void deveSetarFalseQuandoRegistroFalhar() throws Exception {
            Transacao transacao = criarTransacao();

            when(delegateExecution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.enviarTransacaoParaRegistro(transacao)).thenReturn(false);

            delegate.execute(delegateExecution);

            verify(delegateExecution).setVariable(TRANSACAO_VARIABLE, false);
        }
    }

    private Transacao criarTransacao() {
        return Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now())
                .historico(new LinkedHashMap<>())
                .build();
    }
}
