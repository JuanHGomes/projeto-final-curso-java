package org.example.transacaoservice.data.conta;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.conta.model.Conta;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
@Order(2)
public class ContaRepositoryJpaImpl implements ContaRepository, TransacaoOperators {

    private final ContaRepositoryDao dao;

    @Override
    public Long getSaldoByNumeroConta(String numeroConta) {
        Optional<Conta> conta = dao.findByNumeroConta(numeroConta);
        return 0L;
    }

    @Override
    public Long getLimiteCreditoByNumeroConta(String numeroConta) {
        return 0L;
    }

    @Override
    //todo mudar para mongo, salvar somente as transacoes
    public void updateSaldo(String numeroConta, Long valor) {
        Optional<Conta> conta = dao.findByNumeroConta(numeroConta);

        conta.ifPresentOrElse(contaEncontrada -> {
            Long saldo = contaEncontrada.getSaldo();
            Long novoSaldo = saldo - valor;
            contaEncontrada.setSaldo(novoSaldo);
            dao.save(contaEncontrada);
        }, () -> {throw new RuntimeException("Conta não encontrada");
        });
    }

    @Override
    public void updateLimiteCredito(String numeroConta, Long valor) {

    }

}
