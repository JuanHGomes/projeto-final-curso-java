package org.example.transacaoservice.data.conta;


import org.example.transacaoservice.business.transacao.model.Transacao;

public interface ContaRepository {
    Long getSaldoByNumeroConta(String numeroConta);

    Long getLimiteCreditoByNumeroConta(String numeroConta);

    boolean updateSaldo(Transacao transacao);

    void setTransacaoToAguardanddoConfirmacao(Transacao transacao);

    boolean updateLimiteCredito(Transacao transacao);
}
