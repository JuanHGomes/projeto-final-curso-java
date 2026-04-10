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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - ValidarFundosDelegate")
class ValidarFundosDelegateUnitTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private DelegateExecution execution;

    private ValidarFundosDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ValidarFundosDelegate(transacaoService);
    }

    @Nested
    @DisplayName("Validação de Fundos")
    class ValidacaoTests {

        @Test
        @DisplayName("Deve validar fundos e setar variaveis com sucesso")
        void deveValidarFundosComSucesso() throws Exception {
            // given
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FUNDOS_SUFICIENTES", true);

            Transacao transacao = criarTransacao(historico);
            Transacao transacaoValidada = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.validarFundos(transacao)).thenReturn(transacaoValidada);

            // when
            delegate.execute(execution);

            // then
            verify(execution).setVariable(TRANSACAO_VARIABLE, transacaoValidada);
            verify(execution).setVariable(IS_FUNDOS_SUFICIENTES_VARIABLE, true);
            verify(execution).setVariable(RESULTADO_TRANSACAO, true);
        }

        @Test
        @DisplayName("Deve setar variaveis como false quando fundos insuficientes")
        void deveSetarFalseQuandoFundosInsuficientes() throws Exception {
            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FUNDOS_SUFICIENTES", false);

            Transacao transacao = criarTransacao(historico);
            Transacao transacaoValidada = criarTransacao(historico);

            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(transacao);
            when(transacaoService.validarFundos(transacao)).thenReturn(transacaoValidada);

            delegate.execute(execution);

            verify(execution).setVariable(IS_FUNDOS_SUFICIENTES_VARIABLE, false);
            verify(execution).setVariable(RESULTADO_TRANSACAO, false);
        }

        @Test
        @DisplayName("Deve lancar excecao quando transacao validada for null")
        void deveLancarExcecaoQuandoTransacaoNull() {
            when(execution.getVariable(TRANSACAO_VARIABLE)).thenReturn(criarTransacao(new LinkedHashMap<>()));
            when(transacaoService.validarFundos(any())).thenReturn(null);

            org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> delegate.execute(execution));
        }
    }

    private Transacao criarTransacao(LinkedHashMap<String, Boolean> historico) {
        return Transacao.builder()
                .numeroConta("123456")
                .valor(1000L)
                .tipoTransacao(TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja Teste")
                .historico(historico)
                .build();
    }
}
