package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.messaging.TransacaoProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class TransacaoRepository {
    private static final String TOPICO_REGISTRO_TRANSACAO = "registroTransacaoProducer-out-0";

    private final TransacaoRestClient client;
    private final TransacaoProducer producer;

    public Transacao validarFundos(Transacao transacao) {
        return client.validarFundos(transacao);
    }

    public Transacao validarFraude(Transacao transacao) {
       return client.validarFraude(transacao);
    }

    public Transacao executarTransacao(Transacao transacao) {
        return client.executarTransacao(transacao);
    }

    public boolean enviarTransacaoParaRegistro(Transacao transacao) {
        return producer.sendMessage(TOPICO_REGISTRO_TRANSACAO, transacao);
    }
}
