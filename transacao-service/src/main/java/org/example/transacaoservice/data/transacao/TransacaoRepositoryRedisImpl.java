package org.example.transacaoservice.data.transacao;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
@Primary
@Order(2)
public class TransacaoRepositoryRedisImpl implements TransacaoRepository{
    private static final String TRANSACAO_PREFIX = "transacao:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public Transacao save(Transacao transacao) {
        return null;
    }

    @Override
    public Optional<LocalDateTime> getTimeStampByNumeroConta(String numeroConta) {
        String json = (String) redisTemplate.opsForValue()
                .get(TRANSACAO_PREFIX+numeroConta);

        if(json == null){
            return Optional.empty();
        }

        Transacao transacao = objectMapper.readValue(json, Transacao.class);

        return Optional.ofNullable(transacao.getTimeStamp());
    }

    @Override
    public Optional<Transacao> getTransacaoByNumeroConta(String numeroConta) {

        String json = (String) redisTemplate.opsForValue()
                .get(TRANSACAO_PREFIX+numeroConta);

        if(json == null){
            return Optional.empty();
        }

        Transacao transacao = objectMapper.readValue(json, Transacao.class);

        return Optional.of(transacao);
    }
}
