package org.example.transacaoservice.data.conta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@RequiredArgsConstructor
@Repository
@Order(1)
public class ContaRepositoryRedisImpl implements ContaRepository, TransacaoOperators {
    private static final String CONTA_PREFIX = "numeroConta:";

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
    public void updateSaldo(Transacao transacao) {
        log.info("Iniciando atualização do saldo no Redis");
       redisTemplate.opsForHash().increment(
               CONTA_PREFIX+transacao.getNumeroConta(), "saldo", -transacao.getValor());
    }

    @Override
    public void updateLimiteCredito(Transacao transacao) {
        redisTemplate.opsForHash().increment(
                CONTA_PREFIX+transacao.getNumeroConta(), "limiteCredito", -transacao.getValor());
    }
}
