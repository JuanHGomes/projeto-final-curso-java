package org.example.transacaoservice.business.transacao.operators;

import org.example.transacaoservice.business.transacao.model.Transacao;

public interface TransacaoOperators {
    public boolean updateSaldo(Transacao transacao);
    public boolean updateLimiteCredito(Transacao transacao);
}
