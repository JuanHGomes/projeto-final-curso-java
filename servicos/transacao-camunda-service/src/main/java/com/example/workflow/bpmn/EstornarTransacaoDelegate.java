package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;

@Slf4j
@RequiredArgsConstructor
@Component
public class EstornarTransacaoDelegate implements JavaDelegate {

    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando delegate de estorno de transação");
        Transacao transacao = (Transacao) execution.getVariable(TRANSACAO_VARIABLE);
        transacaoService.estornarTransacao(transacao);
        log.info("Estorno concluído com sucesso para a conta: {}", transacao.getNumeroConta());
    }
}
