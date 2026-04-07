package com.example.workflow.api;

import com.example.workflow.api.model.TransacaoRequest;
import com.example.workflow.business.model.Transacao;
import org.springframework.stereotype.Component;

@Component
public class TransacaoMapper {
    public Transacao toTransacao(TransacaoRequest request){
        return Transacao.builder()
                .numeroConta(request.numeroConta())
                .valor(request.valor())
                .tipoTransacao(request.tipoTransacao())
                .timeStamp(request.timeStamp())
                .estabelecimento(request.estabelecimento())
                .historico(request.historico())
                .build();
    }
}
