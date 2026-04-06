package org.example.transacaoservice.business.conta.model;

import lombok.Getter;

@Getter
public class Conta {
    private String numeroConta;
    private Long saldo;
    private Long limiteCredito;

    public void setSaldo(Long saldo) {
        this.saldo = saldo;
    }
}
