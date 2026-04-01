package org.example.transacaoservice.data.conta;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
@Order(1)
public class ContaRepositoryRedisImpl implements ContaRepository, TransacaoOperators {
    private static final String CONTA_PREFIX = "conta:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long getSaldoByNumeroConta(String numeroConta) {
        Long saldo = (Long) redisTemplate.opsForHash().get(CONTA_PREFIX+numeroConta, "saldo");
        return saldo;
    }

    @Override
    public Long getLimiteCreditoByNumeroConta(String numeroConta) {
        return (Long) redisTemplate.opsForHash().get(CONTA_PREFIX+numeroConta, "limiteCredito");
    }

    @Override
    public void updateSaldo(String numeroConta, Long valor) {
       redisTemplate.opsForHash().increment(CONTA_PREFIX+numeroConta, "saldo", -valor);
    }

    @Override
    public void updateLimiteCredito(String numeroConta, Long valor) {
        redisTemplate.opsForHash().increment(CONTA_PREFIX+numeroConta, "limiteCredito", -valor);
    }
}
