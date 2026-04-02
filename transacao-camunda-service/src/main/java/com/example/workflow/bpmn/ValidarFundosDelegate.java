package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;


public class ValidarFundosDelegate implements JavaDelegate {
    public ValidarFundosDelegate(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Transacao transacao = (Transacao) execution.getVariable("TRANSACAO");
        transacaoService.validarFundos(transacao);
    }
}
