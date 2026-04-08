package org.example.transacaoservice.business.transacao.operators;

import org.example.transacaoservice.business.transacao.model.Transacao;

public interface TransacaoOperators {
    public void confirmarTransacao(Transacao transacao);
    public void estornarTransacao(Transacao transacao);
}
