package org.example.transacaoservice.data.transacao;

import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.transacao.model.TransacaoDocument;
import org.springframework.stereotype.Component;

@Component
public class TransacaoMapper {
    public TransacaoDocument toDocument(Transacao transacao){
        return TransacaoDocument.builder()
                .numeroConta(transacao.getNumeroConta())
                .valor(transacao.getValor())
                .tipoTransacao(transacao.getTipoTransacao())
                .timeStamp(transacao.getTimeStamp())
                .estabelecimento(transacao.getEstabelecimento())
                .aprovada(transacao.isAprovada())
                .build();
    }

    public Transacao toTransacao(TransacaoDocument transacaoDocument){
        return Transacao.builder()
                .numeroConta(transacaoDocument.getNumeroConta())
                .valor(transacaoDocument.getValor())
                .tipoTransacao(transacaoDocument.getTipoTransacao())
                .timeStamp(transacaoDocument.getTimeStamp())
                .estabelecimento(transacaoDocument.getEstabelecimento())
                .aprovada(transacaoDocument.isAprovada())
                .build();
    }
}
