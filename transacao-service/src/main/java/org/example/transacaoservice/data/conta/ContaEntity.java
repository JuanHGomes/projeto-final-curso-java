package org.example.transacaoservice.data.conta;

import org.springframework.data.annotation.Id;

@Entity
public class ContaEntity {
    @Id
    private String numeroConta;
    private Long saldo;
    private Long limiteCredito;
}
