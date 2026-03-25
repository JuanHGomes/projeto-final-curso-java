package org.example.transacaoservice.data.conta;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ContaRepositoryRedisImpl implements ContaRepository{
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long getSaldoByNumeroConta(String numeroConta) {
        Long saldo = (Long) redisTemplate.opsForHash().get(numeroConta, "saldo");
        return saldo;
    }

    @Override
    public Long getLimiteCreditoByNumeroConta(String numeroConta) {
        return (Long) redisTemplate.opsForHash().get(numeroConta, "limiteCredito");
    }
}
