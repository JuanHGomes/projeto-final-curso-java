package org.example.registrotransacaoservice.data;

import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class RegistroTransacaoMapper {
    public Transacao toTransacao(TransacaoDocument document){
        return Transacao.builder().numeroConta(document.getNumeroConta())
                .valor(document.getValor())
                .tipoTransacao(document.getTipoTransacao())
                .timeStamp(document.getTimeStamp())
                .estabelecimento(document.getEstabelecimento())
                .historico((LinkedHashMap<String, Boolean>) document.getHistorico())
                .build();
    }

    public TransacaoDocument toDocument(Transacao transacao) {
        return TransacaoDocument.builder()
                .numeroConta(transacao.getNumeroConta())
                .valor(transacao.getValor())
                .tipoTransacao(transacao.getTipoTransacao())
                .timeStamp(transacao.getTimeStamp())
                .estabelecimento(transacao.getEstabelecimento())
                .historico(transacao.getHistorico())
                .build();
    }
}
