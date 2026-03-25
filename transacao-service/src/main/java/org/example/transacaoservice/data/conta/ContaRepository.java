package org.example.transacaoservice.data.conta;

import org.springframework.stereotype.Repository;

@Repository
public interface ContaRepository {
    Long getSaldoByNumeroConta(String numeroConta);
}
