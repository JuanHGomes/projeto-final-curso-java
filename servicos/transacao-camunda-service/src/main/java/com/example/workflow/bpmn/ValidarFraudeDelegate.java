package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.workflow.commons.Camundakeys.IS_FRAUDE_VARIABLE;
import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidarFraudeDelegate implements JavaDelegate {
    private static final String FRAUDE_KEY = "FRAUDE";

    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        log.info("Inciando validação de fraude");
        Transacao transacao = (Transacao) execution.getVariable("TRANSACAO");
        Transacao transacaoValidada = transacaoService.validarFraude(transacao);

        boolean isFraude = transacaoValidada.getHistorico().get(FRAUDE_KEY);
        log.info("Validacao de frude finalizada, resultado: isFraude = {}", isFraude);

        execution.setVariable(TRANSACAO_VARIABLE, transacaoValidada);
        execution.setVariable(IS_FRAUDE_VARIABLE, isFraude);
    }
}
