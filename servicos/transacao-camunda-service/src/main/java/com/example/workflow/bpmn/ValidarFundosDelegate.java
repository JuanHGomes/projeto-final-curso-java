package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.workflow.commons.Camundakeys.IS_FUNDOS_SUFICIENTES_VARIABLE;
import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;


@Slf4j
@Component
public class ValidarFundosDelegate implements JavaDelegate {
    private static final String FUNDOS_KEYS = "FUNDOS_SUFICIENTES";

    private final TransacaoService transacaoService;

    public ValidarFundosDelegate(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Inciando validação de fundos");
        Transacao transacao = (Transacao) execution.getVariable(TRANSACAO_VARIABLE);
        Transacao transacaoValidada = transacaoService.validarFundos(transacao);

        boolean fundosSuficientes = transacaoValidada.getHistorico().get(FUNDOS_KEYS);
        log.info("Validacao de fundos finalizada, resultado: {}", fundosSuficientes);

        execution.setVariable(TRANSACAO_VARIABLE, transacaoValidada);
        execution.setVariable(IS_FUNDOS_SUFICIENTES_VARIABLE, fundosSuficientes);
    }
}
