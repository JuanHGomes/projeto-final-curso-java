package org.example.transacaoservice.data.transacao;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.model.Transacao;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class TransacaoRedisRepositoryImpl implements TransacaoRepository{
    private static final String TRANSACAO_PREFIX = "transacao:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Transacao save(Transacao transacao) {
        return null;
    }

    @Override
    public Optional<LocalDateTime> getTimeStampByNumeroConta(String numeroConta) {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = (String) redisTemplate.opsForValue()
                .get(TRANSACAO_PREFIX+numeroConta);
        Transacao transacao = objectMapper.readValue(json, Transacao.class);

        return Optional.ofNullable(transacao.getTimeStamp());
    }
}
