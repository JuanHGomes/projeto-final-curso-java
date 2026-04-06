package org.example.transacaoservice.api;

import org.example.transacaoservice.api.model.TransacaoRequest;
import org.example.transacaoservice.api.model.TransacaoResponse;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.springframework.stereotype.Component;

@Component
public class TransacaoApiMapper {
    public TransacaoResponse toResponse(Transacao transacao){
        return TransacaoResponse.builder()
                .numeroConta(transacao.getNumeroConta())
                .valor(transacao.getValor())
                .tipoTransacao(transacao.getTipoTransacao())
                .timeStamp(transacao.getTimeStamp())
                .estabelecimento(transacao.getEstabelecimento())
                .historico(transacao.getHistorico())
                .build();
    }

    public Transacao toTransacao(TransacaoRequest transacaoRequest){
        return Transacao.builder()
                .numeroConta(transacaoRequest.numeroConta())
                .valor(transacaoRequest.valor())
                .tipoTransacao(transacaoRequest.tipoTransacao())
                .timeStamp(transacaoRequest.timeStamp())
                .estabelecimento(transacaoRequest.estabelecimento())
                .historico(transacaoRequest.historico())
                .build();
    }
}
