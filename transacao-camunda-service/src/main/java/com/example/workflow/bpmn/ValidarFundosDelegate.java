package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;


public class ValidarFundosDelegate implements JavaDelegate {

    private static final String FUNDOS_KEYS = "FUNDOS_SUFICIENTES";
    private final TransacaoService transacaoService;

    public ValidarFundosDelegate(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Transacao transacao = (Transacao) execution.getVariable("TRANSACAO");
        Transacao transacaoValidada = transacaoService.validarFundos(transacao);

        boolean fundosSuficientes = transacaoValidada.getHistorico().get(FUNDOS_KEYS);

        execution.setVariable(FUNDOS_KEYS, fundosSuficientes);
    }
}
