package org.example.transacaoservice.business.transacao.operators;

public interface TransacaoOperators {
    public void updateSaldo(String numeroConta, Long valor);
    public void updateLimiteCredito(String numeroConta, Long valor);
}
