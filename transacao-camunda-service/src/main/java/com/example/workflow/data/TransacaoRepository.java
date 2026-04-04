package com.example.workflow.data;

import com.example.workflow.business.model.Transacao;
import org.springframework.stereotype.Repository;

@Repository
public class TransacaoRepository {
    private final TransacaoRestClient client;

    public TransacaoRepository(TransacaoRestClient client) {
        this.client = client;
    }

    public Transacao validarFundos(Transacao transacao) {
        return client.validarFundos(transacao);
    }
}
