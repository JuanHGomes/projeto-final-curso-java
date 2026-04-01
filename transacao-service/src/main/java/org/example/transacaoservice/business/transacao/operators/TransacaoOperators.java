package org.example.transacaoservice.business.transacao.operators;

import org.example.transacaoservice.business.transacao.model.Transacao;

public interface TransacaoOperators {
    public void updateSaldo(Transacao transacao);
    public void updateLimiteCredito(Transacao transacao);
}
