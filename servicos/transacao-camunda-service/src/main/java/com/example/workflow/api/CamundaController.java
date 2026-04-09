package com.example.workflow.api;

import com.example.workflow.api.model.TransacaoRequest;
import com.example.workflow.business.enums.TipoTransacao;
import com.example.workflow.business.model.Transacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.workflow.commons.Camundakeys.IS_EXECUCAO_SUCESSO_VARIABLE;
import static com.example.workflow.commons.Camundakeys.RESULTADO_TRANSACAO;
import static com.example.workflow.commons.Camundakeys.TRANSACAO_VARIABLE;

@Slf4j
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController
@RequestMapping("camundaTeste/")
public class CamundaController {
    private final TransacaoMapper mapper;
    private final RuntimeService runtimeService;

    @PostMapping
    public ResponseEntity<Boolean> Start(@RequestBody TransacaoRequest request) {
        Transacao transacao = mapper.toTransacao(request);
        Map<String, Object> variaveis = new HashMap<>();

        variaveis.put("TRANSACAO", transacao);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transacao-process", variaveis);
        Transacao transacaoResultado = (Transacao) runtimeService.getVariable(processInstance.getId(), TRANSACAO_VARIABLE);
        if(transacaoResultado == null){
            log.info("TransacaoResultado null.");
            return ResponseEntity.ok(false);
        }
        LinkedHashMap<String, Boolean> historico = (LinkedHashMap<String, Boolean>) transacaoResultado.getHistorico();

        boolean resultadoExecucao = historico.get(IS_EXECUCAO_SUCESSO_VARIABLE);

        return ResponseEntity.ok(resultadoExecucao);
    }
}
