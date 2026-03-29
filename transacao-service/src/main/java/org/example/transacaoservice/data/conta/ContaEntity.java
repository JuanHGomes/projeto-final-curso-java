package org.example.transacaoservice.data.conta;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ContaEntity {
    @Id
    private String numeroConta;
    private Long saldo;
    private Long limiteCredito;
}
