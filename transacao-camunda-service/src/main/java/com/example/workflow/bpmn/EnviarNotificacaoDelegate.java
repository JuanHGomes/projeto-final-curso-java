package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.AtualizacaoHistorico;
import com.example.workflow.business.model.Transacao;
import jakarta.ws.rs.core.Link;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class EnviarNotificacaoDelegate implements JavaDelegate {
    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Transacao transacao = (Transacao) execution.getVariable("TRANSACAO");
        transacaoService.enviarNotificacao(transacao);
    }

}
