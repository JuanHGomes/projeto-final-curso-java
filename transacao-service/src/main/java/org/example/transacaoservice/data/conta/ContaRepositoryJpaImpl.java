package org.example.transacaoservice.data.conta;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.conta.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ContaRepositoryJpaImpl implements ContaRepository{

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
    public boolean updateSaldo(String numeroConta, Long valor) {
        Optional<Conta> conta = dao.findByNumeroConta(numeroConta);

        conta.ifPresentOrElse(contaEncontrada -> {
            Long saldo = contaEncontrada.getSaldo();
            Long novoSaldo = saldo - valor;
            contaEncontrada.setSaldo(novoSaldo);
            dao.save(contaEncontrada);
        }, () -> {throw new RuntimeException("Conta não encontrada");
        });

        return true;
    }
}
