package com.example.workflow.bpmn;

import com.example.workflow.business.TransacaoService;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.workflow.commons.Camundakeys.IS_EXECUCAO_SUCESSO_VARIABLE;
import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExecutarTransacaoDelegate implements JavaDelegate {
    private static final String EXECUCAO_KEY = "EXECUCAO_SUCESSO";

    private final TransacaoService transacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando execução de transação");
        Transacao transacao =  (Transacao) execution.getVariable("TRANSACAO");
        Transacao transacaoExecutada = transacaoService.executarTransacao(transacao);

        boolean transacaoExecutadaComSucesso = transacaoExecutada.getHistorico().get(EXECUCAO_KEY);
        if(!transacaoExecutadaComSucesso){
            log.info("Transacao não foi executada");
        }

        log.info("Transacao executada com sucesso");

        execution.setVariable(TRANSACAO_VARIABLE, transacaoExecutada);
        execution.setVariable(IS_EXECUCAO_SUCESSO_VARIABLE, transacaoExecutadaComSucesso);
    }
}
