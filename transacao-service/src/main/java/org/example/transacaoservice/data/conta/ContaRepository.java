package org.example.transacaoservice.data.conta;


public interface ContaRepository {
    Long getSaldoByNumeroConta(String numeroConta);

    Long getLimiteCreditoByNumeroConta(String numeroConta);
}
