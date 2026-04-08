package org.example.transacaoservice.data.conta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Repository
@Order(1)
public class ContaRepositoryRedisImpl implements ContaRepository, TransacaoOperators {
    private static final String CONTA_PREFIX = "numeroConta:";
    private static final String TRANSACAO_PREFIX = "transacao:";
    private static final String AGUARDANDO_CONFIRMACAO_PREFIX = "confirmacao:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Long getSaldoByNumeroConta(String numeroConta) {
        Optional<Object> value = Optional.ofNullable(redisTemplate.opsForHash().get(CONTA_PREFIX + numeroConta, "saldo"));
        return value
                .map(Object::toString)
                .map(Long::valueOf)
                .orElse(0L);
    }

    @Override
    public Long getLimiteCreditoByNumeroConta(String numeroConta) {
        Optional<Object> value = Optional.ofNullable(redisTemplate.opsForHash().get(CONTA_PREFIX + numeroConta, "limiteCredito"));
        return value
                .map(Object::toString)
                .map(Long::valueOf)
                .orElse(0L);
    }

    @Override
    public boolean updateSaldo(Transacao transacao) {
        String keyConta = CONTA_PREFIX + transacao.getNumeroConta();

        Long saldoAtualizado = redisTemplate.opsForHash().increment(keyConta, "saldo", -transacao.getValor());

        if (saldoAtualizado != null && saldoAtualizado < 0) {
            log.info("Saldo atualizado apos a transacao = {}, nao e possivel seguir");
            redisTemplate.opsForHash().increment(keyConta, "saldo", transacao.getValor());
            return false;
        }

        return true;
    }

    @Override
    public void setTransacaoToAguardanddoConfirmacao(Transacao transacao) {
        String numeroConta = transacao.getNumeroConta();
        redisTemplate.opsForValue().set(AGUARDANDO_CONFIRMACAO_PREFIX + numeroConta, objectMapper.writeValueAsString(transacao));
    }

    @Override
    public boolean updateLimiteCredito(Transacao transacao) {
        String contaKey = CONTA_PREFIX + transacao.getNumeroConta();

        Long limiteAtualizado = redisTemplate.opsForHash().increment(contaKey, "limiteCredito", -transacao.getValor());

        if (limiteAtualizado != null && limiteAtualizado < 0) {
            redisTemplate.opsForHash().increment(contaKey, "limiteCredito", transacao.getValor());
            return false;
        }

        return true;
    }

    @Override
    public void confirmarTransacao(Transacao transacao) {
        String transacaoKey = TRANSACAO_PREFIX + transacao.getNumeroConta();
        String aguardandoConfirmacaoKey = AGUARDANDO_CONFIRMACAO_PREFIX + transacao.getNumeroConta();

        redisTemplate.opsForValue()
                .set(transacaoKey, objectMapper.writeValueAsString(transacao), 10, TimeUnit.MINUTES);

        redisTemplate.delete(aguardandoConfirmacaoKey);

        log.info("Confirmação de transação no Redis para a conta: {}", transacao.getNumeroConta());
    }

    @Override
    public void estornarTransacao(Transacao transacao) {
        String contaKey = CONTA_PREFIX + transacao.getNumeroConta();

        log.info("Estornando transação no Redis para a conta: {}", transacao.getNumeroConta());

        if (org.example.transacaoservice.enums.TipoTransacao.DEBITO.equals(transacao.getTipoTransacao())) {
            redisTemplate.opsForHash().increment(contaKey, "saldo", transacao.getValor());
        } else {
            redisTemplate.opsForHash().increment(contaKey, "limiteCredito", transacao.getValor());
        }
    }
}
