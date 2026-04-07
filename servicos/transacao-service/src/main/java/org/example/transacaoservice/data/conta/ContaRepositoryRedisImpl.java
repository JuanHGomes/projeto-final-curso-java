package org.example.transacaoservice.data.conta;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Repository
@Order(1)
public class ContaRepositoryRedisImpl implements ContaRepository, TransacaoOperators {
    private static final String CONTA_PREFIX = "numeroConta:";
    private static final String TRANSACAO_PREFIX = "transacao:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

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
        String key = CONTA_PREFIX + transacao.getNumeroConta();
        String lockKey = TRANSACAO_PREFIX + transacao.getNumeroConta();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("Já existe uma transação em curso para a conta: {}", transacao.getNumeroConta());
            return false;
        }

        Long saldoAtualizado = redisTemplate.opsForHash().increment(key, "saldo", -transacao.getValor());

        if (saldoAtualizado != null && saldoAtualizado < 0) {
            redisTemplate.opsForHash().increment(key, "saldo", transacao.getValor());
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(transacao);
            redisTemplate.opsForValue().set(lockKey, json, 10, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            log.error("Erro ao salvar reserva no Redis", e);
            redisTemplate.opsForHash().increment(key, "saldo", transacao.getValor());
            return false;
        }
    }

    @Override
    public boolean updateLimiteCredito(Transacao transacao) {
        String key = CONTA_PREFIX + transacao.getNumeroConta();
        String lockKey = TRANSACAO_PREFIX + transacao.getNumeroConta();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            return false;
        }

        Long limiteAtualizado = redisTemplate.opsForHash().increment(key, "limiteCredito", -transacao.getValor());

        if (limiteAtualizado != null && limiteAtualizado < 0) {
            redisTemplate.opsForHash().increment(key, "limiteCredito", transacao.getValor());
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(transacao);
            redisTemplate.opsForValue().set(lockKey, json, 10, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            redisTemplate.opsForHash().increment(key, "limiteCredito", transacao.getValor());
            return false;
        }
    }

    @Override
    public void confirmarTransacao(Transacao transacao) {
        String lockKey = TRANSACAO_PREFIX + transacao.getNumeroConta();
        redisTemplate.delete(lockKey);
        log.info("Confirmação de transação no Redis para a conta: {}", transacao.getNumeroConta());
    }

    @Override
    public void estornarTransacao(Transacao transacao) {
        String key = CONTA_PREFIX + transacao.getNumeroConta();
        String lockKey = TRANSACAO_PREFIX + transacao.getNumeroConta();

        log.info("Estornando transação no Redis para a conta: {}", transacao.getNumeroConta());

        if (org.example.transacaoservice.enums.TipoTransacao.DEBITO.equals(transacao.getTipoTransacao())) {
            redisTemplate.opsForHash().increment(key, "saldo", transacao.getValor());
        } else {
            redisTemplate.opsForHash().increment(key, "limiteCredito", transacao.getValor());
        }

        redisTemplate.delete(lockKey);
    }
}
