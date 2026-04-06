package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;

@RequiredArgsConstructor
@Component
public class EnviarRegistroTransacaoDelegate implements JavaDelegate {
    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Transacao transacao = (Transacao) delegateExecution.getVariable(TRANSACAO_VARIABLE);
        boolean transacaoRegistrada = transacaoService.enviarTransacaoParaRegistro(transacao);

        delegateExecution.setVariable(TRANSACAO_VARIABLE, transacaoRegistrada);
    }
}
