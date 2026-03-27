package org.example.transacaoservice.data.conta;

import org.example.transacaoservice.business.conta.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaRepositoryDao extends JpaRepository<Conta, String> {
    Optional<Conta> findByNumeroConta(String numeroConta);
}
