package org.example.transacaoservice.data.conta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Repository
@Order(1)
public class ContaRepositoryRedisImpl implements ContaRepository, TransacaoOperators {
    private static final String CONTA_PREFIX = "numeroConta:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long getSaldoByNumeroConta(String numeroConta) {
       Optional<Object> value = Optional.ofNullable(redisTemplate.opsForHash().get(CONTA_PREFIX+numeroConta, "saldo"));
      return value
              .map(Object::toString)
              .map(Long::valueOf)
              .orElse(0L);
    }

    @Override
    public Long getLimiteCreditoByNumeroConta(String numeroConta) {
        Optional<Object> value = Optional.ofNullable(redisTemplate.opsForHash().get(CONTA_PREFIX+numeroConta, "limiteCredito"));
        return value
                .map(Object::toString)
                .map(Long::valueOf)
                .orElse(0L);
    }

    @Override
    public boolean updateSaldo(Transacao transacao) {
        log.info("Iniciando atualização do saldo no Redis");
       Long saldoAtualizado = redisTemplate.opsForHash().increment(
               CONTA_PREFIX+transacao.getNumeroConta(), "saldo", -transacao.getValor());

       return saldoAtualizado != null;
    }

    @Override
    public boolean updateLimiteCredito(Transacao transacao) {
        Long limiteCreditoAtualizado = redisTemplate.opsForHash().increment(
                CONTA_PREFIX+transacao.getNumeroConta(), "limiteCredito", -transacao.getValor());

        return limiteCreditoAtualizado != null;
    }
}
