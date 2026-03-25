package org.example.transacaoservice.business.conta.model;

import lombok.Getter;

@Getter
public class Conta {
    private String numeroConta;
    private Long saldo;
    private Long limiteCredito;
}
